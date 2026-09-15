package hgds.epicgrief.arrows;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArrowItemService {

    private final ArrowConfigService configService;
    private final MessageService messages;
    private final NamespacedKey itemKey;
    private final NamespacedKey projectileKey;

    public ArrowItemService(JavaPlugin plugin, ArrowConfigService configService, MessageService messages) {
        this.configService = configService;
        this.messages = messages;
        this.itemKey = new NamespacedKey(plugin, "arrow_uuid");
        this.projectileKey = new NamespacedKey(plugin, "projectile_arrow_uuid");
    }

    public ItemStack createItem(ArrowDefinition arrow, int amount) {
        ItemStack item = new ItemStack(arrow.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String title = messages.color(arrow.title());
        if (!title.isBlank()) {
            meta.setDisplayName(title);
        }

        List<String> lore = messages.colorList(arrow.lore());
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (arrow.color() != null) {
                potionMeta.setColor(arrow.color());
            }

            for (PotionEffect effect : arrow.effects()) {
                potionMeta.addCustomEffect(effect, true);
            }
        }

        for (String rawFlag : arrow.flags()) {
            parseItemFlag(rawFlag).ifPresent(meta::addItemFlags);
        }

        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, arrow.uuid().toString());
        item.setItemMeta(meta);
        return item;
    }

    public ArrowDefinition matchItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        ArrowDefinition byUuid = arrowFromContainer(meta.getPersistentDataContainer());
        if (byUuid != null) {
            return byUuid;
        }

        for (ArrowDefinition arrow : configService.arrows()) {
            if (matchesConfiguredItem(item, meta, arrow)) {
                return arrow;
            }
        }

        return null;
    }

    public void tagProjectile(AbstractArrow projectile, ArrowDefinition arrow) {
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, arrow.uuid().toString());
    }

    public ArrowDefinition matchProjectile(AbstractArrow projectile) {
        return arrowFromContainer(projectile.getPersistentDataContainer());
    }

    public void giveItem(org.bukkit.entity.Player player, ArrowDefinition arrow, int amount) {
        int remaining = Math.max(1, amount);
        int maxStackSize = Math.max(1, new ItemStack(arrow.material()).getMaxStackSize());

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            ItemStack stack = createItem(arrow, stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= stackAmount;
        }

        player.updateInventory();
    }

    private ArrowDefinition arrowFromContainer(PersistentDataContainer container) {
        String rawUuid = container.get(itemKey, PersistentDataType.STRING);
        if (rawUuid == null) {
            rawUuid = container.get(projectileKey, PersistentDataType.STRING);
        }

        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }

        try {
            return configService.arrow(UUID.fromString(rawUuid));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean matchesConfiguredItem(ItemStack item, ItemMeta meta, ArrowDefinition arrow) {
        if (item.getType() != arrow.material()) {
            return false;
        }

        String configuredTitle = messages.color(arrow.title());
        if (!configuredTitle.isBlank() && (!meta.hasDisplayName() || !Objects.equals(meta.getDisplayName(), configuredTitle))) {
            return false;
        }

        List<String> configuredLore = messages.colorList(arrow.lore());
        if (!configuredLore.isEmpty()) {
            return meta.hasLore() && Objects.equals(meta.getLore(), configuredLore);
        }

        return true;
    }

    private Optional<ItemFlag> parseItemFlag(String rawFlag) {
        if (rawFlag == null || rawFlag.isBlank()) {
            return Optional.empty();
        }

        String normalized = rawFlag.trim().toUpperCase(Locale.ROOT);
        for (String candidate : flagCandidates(normalized)) {
            try {
                return Optional.of(ItemFlag.valueOf(candidate));
            } catch (IllegalArgumentException ignored) {
                // Try the next known Bukkit/Paper name.
            }
        }

        return Optional.empty();
    }

    private List<String> flagCandidates(String flag) {
        if ("HIDE_POTION_EFFECTS".equals(flag)) {
            return List.of("HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");
        }

        return List.of(flag);
    }
}
