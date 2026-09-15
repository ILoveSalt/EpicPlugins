package hgds.epicgrief;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class PotionStackManager implements Listener {

    private static final int MAX_COMPONENT_STACK_SIZE = 99;

    private final EpicPotions plugin;
    private final int potionStackSize;
    private final int splashPotionStackSize;
    private final double mergeRadius;
    private final boolean inventoryStacking;
    private final Set<UUID> pendingPlayerScans = new HashSet<>();
    private boolean playerScanScheduled;

    PotionStackManager(EpicPotions plugin) {
        this.plugin = plugin;
        this.potionStackSize = readStackSize("potions_stack", 64);
        this.splashPotionStackSize = readStackSize("splash_potions_stack", 32);
        this.mergeRadius = Math.max(0.0, plugin.getConfig().getDouble("near_stack_potions", 3.0));
        this.inventoryStacking = plugin.getConfig().getBoolean("inventory_stack", true);
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::scanOnlinePlayers, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::scanDroppedPotions, 1L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        scanPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        schedulePlayerScan(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> schedulePlayerScan(player), 4L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        Bukkit.getOnlinePlayers().forEach(this::schedulePlayerScan);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        scanInventory(event.getInventory());
        if (event.getPlayer() instanceof Player player) {
            scanPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        configurePotion(event.getCurrentItem());
        configurePotion(event.getCursor());
        if (event.getWhoClicked() instanceof Player player) {
            schedulePlayerScan(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        configurePotion(event.getOldCursor());
        event.getNewItems().values().forEach(this::configurePotion);
        if (event.getWhoClicked() instanceof Player player) {
            schedulePlayerScan(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        event.getResults().forEach(this::configurePotion);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        event.getLoot().forEach(this::configurePotion);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!isPotion(event.getItem())) {
            return;
        }

        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        Bukkit.getScheduler().runTask(plugin, () -> {
            scanInventory(source);
            scanInventory(destination);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        configureDroppedPotion(event.getItem());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        configureDroppedPotion(event.getItem());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (configurePotion(item)) {
            event.setItem(item);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        configurePotion(event.getItem());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        configureDroppedPotion(item);
        Bukkit.getScheduler().runTask(plugin, () -> mergeNearby(item));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        configureChunk(event.getChunk());
    }

    private void schedulePlayerScan(Player player) {
        pendingPlayerScans.add(player.getUniqueId());
        if (playerScanScheduled) {
            return;
        }

        playerScanScheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            playerScanScheduled = false;

            Set<UUID> playersToScan = new HashSet<>(pendingPlayerScans);
            pendingPlayerScans.clear();
            for (UUID playerId : playersToScan) {
                Player pendingPlayer = Bukkit.getPlayer(playerId);
                if (pendingPlayer != null && pendingPlayer.isOnline()) {
                    scanPlayer(pendingPlayer);
                }
            }
        });
    }

    private void scanOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(this::scanPlayer);
    }

    private void scanPlayer(Player player) {
        scanInventory(player.getInventory());
        scanInventory(player.getOpenInventory().getTopInventory());

        ItemStack cursor = player.getItemOnCursor();
        if (configurePotion(cursor)) {
            player.setItemOnCursor(cursor);
        }
    }

    private void scanInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (configurePotion(item)) {
                inventory.setItem(slot, item);
            }
        }

        mergePotionStacks(inventory, contents);
    }

    private void mergePotionStacks(Inventory inventory, ItemStack[] contents) {
        if (!inventoryStacking) {
            return;
        }

        for (int targetSlot = 0; targetSlot < contents.length; targetSlot++) {
            ItemStack target = contents[targetSlot];
            if (!isPotion(target)) {
                continue;
            }

            int maxStackSize = getConfiguredStackSize(target.getType());
            if (target.getAmount() >= maxStackSize) {
                continue;
            }

            for (int sourceSlot = targetSlot + 1; sourceSlot < contents.length; sourceSlot++) {
                ItemStack source = contents[sourceSlot];
                if (!isPotion(source) || !target.isSimilar(source)) {
                    continue;
                }

                int moved = Math.min(maxStackSize - target.getAmount(), source.getAmount());
                if (moved <= 0) {
                    break;
                }

                target.setAmount(target.getAmount() + moved);
                inventory.setItem(targetSlot, target);

                if (moved == source.getAmount()) {
                    contents[sourceSlot] = null;
                    inventory.clear(sourceSlot);
                } else {
                    source.setAmount(source.getAmount() - moved);
                    inventory.setItem(sourceSlot, source);
                }

                if (target.getAmount() >= maxStackSize) {
                    break;
                }
            }
        }
    }

    private void scanDroppedPotions() {
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                configureDroppedPotion(item);
                mergeNearby(item);
            }
        }
    }

    private void configureChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item item) {
                configureDroppedPotion(item);
            }
        }
    }

    private void configureDroppedPotion(Item item) {
        ItemStack stack = item.getItemStack();
        if (configurePotion(stack)) {
            item.setItemStack(stack);
        }
    }

    private boolean configurePotion(ItemStack item) {
        if (!inventoryStacking || !isPotion(item)) {
            return false;
        }

        int maxStackSize = getConfiguredStackSize(item.getType());
        ItemMeta meta = item.getItemMeta();
        if (meta.hasMaxStackSize() && meta.getMaxStackSize() == maxStackSize) {
            return false;
        }

        meta.setMaxStackSize(maxStackSize);
        item.setItemMeta(meta);
        return true;
    }

    private void mergeNearby(Item target) {
        if (mergeRadius <= 0.0 || !target.isValid()) {
            return;
        }

        configureDroppedPotion(target);
        ItemStack targetStack = target.getItemStack();
        if (!isPotion(targetStack)) {
            return;
        }

        int maxStackSize = getConfiguredStackSize(targetStack.getType());
        if (targetStack.getAmount() >= maxStackSize) {
            return;
        }

        for (Entity entity : target.getNearbyEntities(mergeRadius, mergeRadius, mergeRadius)) {
            if (!(entity instanceof Item nearby) || !nearby.isValid() || nearby.equals(target)) {
                continue;
            }
            if (!Objects.equals(target.getOwner(), nearby.getOwner())
                    || !Objects.equals(target.getThrower(), nearby.getThrower())) {
                continue;
            }

            configureDroppedPotion(nearby);
            ItemStack nearbyStack = nearby.getItemStack();
            if (!targetStack.isSimilar(nearbyStack)) {
                continue;
            }

            int moved = Math.min(maxStackSize - targetStack.getAmount(), nearbyStack.getAmount());
            if (moved <= 0) {
                break;
            }

            targetStack.setAmount(targetStack.getAmount() + moved);
            target.setItemStack(targetStack);

            if (moved == nearbyStack.getAmount()) {
                nearby.remove();
            } else {
                nearbyStack.setAmount(nearbyStack.getAmount() - moved);
                nearby.setItemStack(nearbyStack);
            }

            if (targetStack.getAmount() >= maxStackSize) {
                break;
            }
        }
    }

    private int readStackSize(String path, int defaultValue) {
        int configured = plugin.getConfig().getInt(path, defaultValue);
        int clamped = Math.max(1, Math.min(MAX_COMPONENT_STACK_SIZE, configured));
        if (configured != clamped) {
            plugin.getLogger().warning(
                    path + " must be between 1 and " + MAX_COMPONENT_STACK_SIZE + "; using " + clamped + ".");
        }
        return clamped;
    }

    private int getConfiguredStackSize(Material material) {
        return material == Material.POTION ? potionStackSize : splashPotionStackSize;
    }

    private boolean isPotion(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        Material material = item.getType();
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }
}
