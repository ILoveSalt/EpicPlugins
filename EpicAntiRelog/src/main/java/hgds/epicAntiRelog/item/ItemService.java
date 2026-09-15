package hgds.epicAntiRelog.item;

import hgds.epicAntiRelog.combat.CombatService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.message.MessageService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ItemService {

    private static final String ESCAPE_ITEM_KEY = "escape_item";

    private final JavaPlugin plugin;
    private final AntiRelogSettings settings;
    private final MessageService messages;
    private final CombatService combatService;
    private final NamespacedKey escapeItemKey;
    private final Map<UUID, Map<String, Long>> itemCooldowns = new HashMap<>();
    private final Set<UUID> pluginTeleports = new HashSet<>();

    public ItemService(
            JavaPlugin plugin,
            AntiRelogSettings settings,
            MessageService messages,
            CombatService combatService
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.combatService = combatService;
        this.escapeItemKey = new NamespacedKey(plugin, ESCAPE_ITEM_KEY);
    }

    public void handleInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (isEscapeItem(item)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            teleportWithConfiguredItem(player, item, event.getHand());
            return;
        }

        if (!combatService.isInCombat(player)) {
            return;
        }

        CooldownRule rule = getCooldownRule(item.getType());
        if (rule != null) {
            applyItemRule(event, player, rule);
        }
    }

    public void handleTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player) || !combatService.isInCombat(player)) {
            return;
        }

        applyResurrectRule(event, player, new CooldownRule("totem-cooldown", "totem", Material.TOTEM_OF_UNDYING));
    }

    public void handleConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = findEscapeItemHand(player);
        ItemStack item = itemInHand(player, hand);

        if (!isEscapeItem(item) && !isEscapeItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        teleportWithConfiguredItem(player, item, hand);
    }

    public void handleTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!settings.disableTeleportsInPvp() || !combatService.isInCombat(player)) {
            return;
        }

        if (pluginTeleports.remove(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
    }

    public void clearPlayerState(Player player) {
        itemCooldowns.remove(player.getUniqueId());
        pluginTeleports.remove(player.getUniqueId());
    }

    public void clear() {
        itemCooldowns.clear();
        pluginTeleports.clear();
    }

    public ItemStack createConfiguredItem(int amount) {
        String materialName = settings.string("item.material");
        if (materialName == null || materialName.isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return null;
        }

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String display = messages.color(settings.string("item.display"));
        if (!display.isBlank()) {
            meta.setDisplayName(display);
        }

        List<String> lore = messages.colorList(settings.stringList("item.lore"));
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        meta.getPersistentDataContainer().set(escapeItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void applyItemRule(PlayerInteractEvent event, Player player, CooldownRule rule) {
        int cooldown = settings.intValue(rule.configPath(), 0);

        if (cooldown < 0) {
            event.setCancelled(true);
            messages.sendConfiguredMessage(player, "messages.item-disabled-in-pvp", combatService.placeholders(player));
            return;
        }

        if (cooldown == 0) {
            return;
        }

        int remainingCooldown = getCooldownRemainingSeconds(player, rule.cooldownKey());
        if (remainingCooldown > 0) {
            event.setCancelled(true);
            messages.sendConfiguredMessage(player, "messages.item-cooldown", messages.playerPlaceholders(player, remainingCooldown));
            return;
        }

        startCooldown(player, rule);
    }

    private void applyResurrectRule(EntityResurrectEvent event, Player player, CooldownRule rule) {
        int cooldown = settings.intValue(rule.configPath(), 0);

        if (cooldown < 0) {
            event.setCancelled(true);
            messages.sendConfiguredMessage(player, "messages.item-disabled-in-pvp", combatService.placeholders(player));
            return;
        }

        if (cooldown == 0) {
            return;
        }

        int remainingCooldown = getCooldownRemainingSeconds(player, rule.cooldownKey());
        if (remainingCooldown > 0) {
            event.setCancelled(true);
            messages.sendConfiguredMessage(player, "messages.item-cooldown", messages.playerPlaceholders(player, remainingCooldown));
            return;
        }

        startCooldown(player, rule);
    }

    private CooldownRule getCooldownRule(Material material) {
        return switch (material) {
            case FIREWORK_ROCKET -> new CooldownRule("firework-cooldown", "firework", material);
            case GOLDEN_APPLE -> new CooldownRule("golden-apple-cooldown", "golden-apple", material);
            case ENCHANTED_GOLDEN_APPLE -> new CooldownRule("enchanted-golden-apple-cooldown", "enchanted-golden-apple", material);
            case ENDER_PEARL -> new CooldownRule("ender-pearl-cooldown", "ender-pearl", material);
            case CHORUS_FRUIT -> new CooldownRule("chorus-cooldown", "chorus", material);
            default -> null;
        };
    }

    private int getCooldownRemainingSeconds(Player player, String key) {
        Long expiresAt = itemCooldowns
                .getOrDefault(player.getUniqueId(), Map.of())
                .get(key);
        if (expiresAt == null) {
            return 0;
        }

        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return 0;
        }

        return (int) Math.ceil(remainingMillis / 1000.0D);
    }

    private void startCooldown(Player player, CooldownRule rule) {
        int seconds = settings.intValue(rule.configPath(), 0);
        long expiresAt = System.currentTimeMillis() + (seconds * 1000L);

        itemCooldowns
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(rule.cooldownKey(), expiresAt);

        player.setCooldown(rule.material(), seconds * CombatService.TICKS_PER_SECOND);
    }

    private boolean isEscapeItem(ItemStack item) {
        String materialName = settings.string("item.material");
        if (materialName == null || materialName.isBlank()) {
            return false;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null || item.getType() != material) {
            return false;
        }

        return true;
    }

    private void teleportWithConfiguredItem(Player player, ItemStack item, EquipmentSlot hand) {
        Location spawn = resolveSpawn(player);
        consumeEscapeItem(player, item, hand);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            pluginTeleports.add(player.getUniqueId());
            try {
                if (player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                    combatService.clearCombat(player, true);
                    messages.sendConfiguredMessage(player, "messages.item-teleported", combatService.placeholders(player));
                }
            } finally {
                pluginTeleports.remove(player.getUniqueId());
            }
        });
    }

    private EquipmentSlot findEscapeItemHand(Player player) {
        if (isEscapeItem(player.getInventory().getItemInMainHand())) {
            return EquipmentSlot.HAND;
        }

        if (isEscapeItem(player.getInventory().getItemInOffHand())) {
            return EquipmentSlot.OFF_HAND;
        }

        return EquipmentSlot.HAND;
    }

    private ItemStack itemInHand(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private Location resolveSpawn(Player player) {
        FileConfiguration spawnConfig = findEpicSpawnConfig();
        if (spawnConfig != null) {
            String worldName = spawnConfig.getString("spawn.world", "");
            if (worldName != null && !worldName.isBlank()) {
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    return new Location(
                            world,
                            spawnConfig.getDouble("spawn.x", world.getSpawnLocation().getX()),
                            spawnConfig.getDouble("spawn.y", world.getSpawnLocation().getY()),
                            spawnConfig.getDouble("spawn.z", world.getSpawnLocation().getZ()),
                            (float) spawnConfig.getDouble("spawn.yaw", world.getSpawnLocation().getYaw()),
                            (float) spawnConfig.getDouble("spawn.pitch", world.getSpawnLocation().getPitch())
                    );
                }
            }
        }

        return player.getWorld().getSpawnLocation();
    }

    private FileConfiguration findEpicSpawnConfig() {
        Plugin spawnPlugin = plugin.getServer().getPluginManager().getPlugin("EpicSpawn");
        if (spawnPlugin instanceof JavaPlugin javaPlugin && spawnPlugin.isEnabled()) {
            return javaPlugin.getConfig();
        }

        File pluginsFolder = plugin.getDataFolder().getParentFile();
        if (pluginsFolder == null) {
            return null;
        }

        File configFile = new File(new File(pluginsFolder, "EpicSpawn"), "config.yml");
        return configFile.isFile() ? YamlConfiguration.loadConfiguration(configFile) : null;
    }

    private void consumeEscapeItem(Player player, ItemStack item, EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (item.getAmount() <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            item.setAmount(item.getAmount() - 1);
        }
        player.updateInventory();
    }
}
