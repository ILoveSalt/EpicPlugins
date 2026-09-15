package hgds.epicgrief.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import hgds.epicgrief.api.Cooldowns;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.CustomItemSlot;
import hgds.epicgrief.api.ItemAPI;
import hgds.epicgrief.database.SQLPickaxe;
import hgds.epicgrief.utils.AntiRelogUtils;
import hgds.epicgrief.utils.Utils;
import hgds.epicgrief.utils.WorldGuardUtils;

public class ItemsListener implements Listener {
   private ItemAPI itemAPI = (ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems");
   private List<String> cancelled = new ArrayList<>();
   private List<String> noDamage = new ArrayList<>();
   private Map<Player, List<ItemStack>> items = new ConcurrentHashMap<>();

   @EventHandler
   public void onAnvil(PrepareAnvilEvent event) {
      ItemStack item = event.getInventory().getItem(0);
      if (item != null) {
         if (this.itemAPI.getItem(item) != null) {
            event.setResult(this.itemAPI.getItem(item).getBukkit());
         }
      }
   }

   @EventHandler
   public void onDamage(EntityDamageEvent e) {
      if (e.getCause() == DamageCause.FALL && this.noDamage.contains(e.getEntity().getName())) {
         e.setCancelled(true);
         this.noDamage.remove(e.getEntity().getName());
      }

   }

   private void addItemToListIfNotNull(List<CustomItem> items, ItemStack itemStack, CustomItemSlot slot) {
      if (itemStack != null && itemStack.getType() != Material.AIR) {
         CustomItem item = this.itemAPI.getItem(itemStack);
         if (item != null && item.isUsedSlot(slot)) {
            items.add(item);
         }
      }

   }

   private List<CustomItem> findUseItems(Player player) {
      List<CustomItem> items = new ArrayList();
      this.addItemToListIfNotNull(items, player.getInventory().getItemInMainHand(), CustomItemSlot.RIGHT_HAND);
      this.addItemToListIfNotNull(items, player.getInventory().getItemInOffHand(), CustomItemSlot.LEFT_HAND);
      this.addItemToListIfNotNull(items, player.getInventory().getHelmet(), CustomItemSlot.HEAD);
      this.addItemToListIfNotNull(items, player.getInventory().getChestplate(), CustomItemSlot.CHEST_PLATE);
      this.addItemToListIfNotNull(items, player.getInventory().getLeggings(), CustomItemSlot.LEGGINGS);
      this.addItemToListIfNotNull(items, player.getInventory().getBoots(), CustomItemSlot.BOOTS);
      return items;
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
      if (e.getEntity() instanceof Player) {
         if (e.getDamager() != null) {
            Player player = (Player)e.getEntity();

            for(CustomItem item : this.findUseItems(player)) {
               if (item.isNoKnockback()) {
                  Bukkit.getScheduler().runTaskLater((Plugin)this.itemAPI, () -> player.setVelocity(new Vector()), 1L);
                  break;
               }
            }

         }
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onTeleportEvent(PlayerTeleportEvent e) {
      if (this.cancelled.contains(e.getPlayer().getName())) {
         e.setCancelled(false);
      }

   }

   @EventHandler
   public void onPlace(BlockPlaceEvent e) {
      CustomItem customItem = this.itemAPI.getItem(e.getItemInHand());
      if (customItem != null) {
         e.setCancelled(true);
      }

   }

   @EventHandler
   public void onRepairItem(PrepareAnvilEvent e) {
      List<ItemStack> items = new ArrayList();
      if (e.getInventory().getItem(0) != null) {
         items.add(e.getInventory().getItem(0));
      }

      if (e.getInventory().getItem(1) != null) {
         items.add(e.getInventory().getItem(1));
      }

      for(ItemStack item : items) {
         CustomItem customItem = this.itemAPI.getItem(item);
         if (customItem != null && customItem.isNoRepair()) {
            e.setResult((ItemStack)null);
         }
      }

   }

   @EventHandler
   public void onPlaceSpawner(BlockPlaceEvent e) {
      ItemStack itemStack = e.getItemInHand();
      Block placeBlock = e.getBlock();
      if (itemStack.getType() == Material.SPAWNER) {
         BlockStateMeta spawnerMeta = (BlockStateMeta)itemStack.getItemMeta();
         CreatureSpawner oldSpawner = (CreatureSpawner)spawnerMeta.getBlockState();
         CreatureSpawner newSpawner = (CreatureSpawner)placeBlock.getState();
         newSpawner.setSpawnedType(oldSpawner.getSpawnedType());
         newSpawner.setSpawnCount(oldSpawner.getSpawnCount());
         newSpawner.setMaxNearbyEntities(oldSpawner.getMaxNearbyEntities());
         newSpawner.setMinSpawnDelay(oldSpawner.getMinSpawnDelay());
         newSpawner.setMaxSpawnDelay(oldSpawner.getMaxSpawnDelay());
         newSpawner.setRequiredPlayerRange(oldSpawner.getRequiredPlayerRange());
         newSpawner.update(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onKick(PlayerKickEvent e) {
      if (Cooldowns.has(e.getPlayer().getName(), "kick")) {
         e.setCancelled(true);
      }

   }

   @EventHandler
   public void onBreakBlock(BlockBreakEvent e) {
      Player player = e.getPlayer();
      CustomItem customItem = this.itemAPI.getItem(e.getPlayer().getInventory().getItemInMainHand());
      if (customItem != null) {
         if (customItem.hasBrokenBlocksToDestroyPickaxe()) {
            List<Material> materials = new ArrayList();
            if (customItem.hasBlocksToDestroyPickaxe()) {
               for(String strMaterial : customItem.getBlocksToDestroyPickaxe()) {
                  Material material = Utils.resolveMaterial(strMaterial);
                  if (material != null) {
                     materials.add(material);
                  }
               }
            }

            if (materials.size() != 0 && materials.contains(e.getBlock().getType()) && WorldGuardUtils.canBuild(player, e.getBlock())) {
               // Защита от дюпа: проверяем что блок еще существует
               if (e.getBlock().getType() == Material.AIR) {
                  e.setCancelled(true);
                  return;
               }
               
               ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
               SQLPickaxe sqlPickaxe = this.itemAPI.getSQLItem(item);
               sqlPickaxe.setBrokenBlocks(sqlPickaxe.getBrokenBlocks() + 1);
               if (sqlPickaxe.getBrokenBlocks() >= customItem.getBrokenBlocksToDestroyPickaxe()) {
                  sqlPickaxe.setDelete(true);
                  item.setAmount(item.getAmount() - 1);
               } else {
                  this.itemAPI.updatePlaceholder(item);
               }

               e.getPlayer().updateInventory();
            }
         }

         if (customItem.isDefaultBreakRadius()) {
            if (e.getBlock().getType() == Material.SPAWNER && customItem.isDropSpawnerAfterBreak() && WorldGuardUtils.canBuild(player, e.getBlock())) {
               // Защита от дюпа: проверяем что блок еще существует
               if (e.getBlock().getType() == Material.AIR) {
                  e.setCancelled(true);
                  return;
               }
               
               Block spawner = e.getBlock();
               Material spawnerMaterial = spawner.getType();
               CreatureSpawner creatureSpawner = (CreatureSpawner)spawner.getState();
               ItemStack itemStack = new ItemStack(spawnerMaterial);
               BlockStateMeta itemMeta = (BlockStateMeta)itemStack.getItemMeta();
               itemMeta.setDisplayName("§bСпавнер " + creatureSpawner.getSpawnedType().name().toLowerCase().replace("_", " "));
               itemMeta.setBlockState(creatureSpawner.getBlock().getState());
               itemStack.setItemMeta(itemMeta);
               spawner.setType(Material.AIR);
               spawner.getWorld().dropItem(spawner.getLocation(), itemStack);
            } else if (customItem.isRemeltingAfterBreak() && WorldGuardUtils.canBuild(player, e.getBlock())) {
               // Защита от дюпа: проверяем что блок еще существует
               if (e.getBlock().getType() == Material.AIR) {
                  e.setCancelled(true);
                  return;
               }
               
               ItemStack smeltItem = Utils.smelt(e.getBlock());
               if (smeltItem != null) {
                  e.setDropItems(false);
                  e.getBlock().getWorld().dropItem(e.getBlock().getLocation(), smeltItem);
               }
            }
         } else {
            Cooldowns.set(e.getPlayer().getName(), "kick", (double)2000.0F);
            Utils.breakBlocks(e.getBlock().getLocation(), customItem, player);
         }
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRespawn(PlayerRespawnEvent e) {
      for(ItemStack customItem : this.items.getOrDefault(e.getPlayer(), new ArrayList<>())) {
         e.getPlayer().getInventory().addItem(new ItemStack[]{customItem});
      }

      this.items.remove(e.getPlayer());
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onPlayerDeathEvent(PlayerDeathEvent e) {
      Player player = e.getEntity();
      List<ItemStack> dropItems = new ArrayList();
      ListIterator<ItemStack> var4 = player.getInventory().iterator();

      while(var4.hasNext()) {
         ItemStack itemStack = (ItemStack)var4.next();
         if (itemStack != null && itemStack.getType() != Material.AIR) {
            CustomItem customItem = this.itemAPI.getItem(itemStack);
            if (customItem != null) {
               if (customItem.isDropAfterDeath()) {
                  dropItems.add(itemStack);
               }

               if (customItem.isSaveAfterDeath()) {
                  List<ItemStack> save = this.items.getOrDefault(player, new ArrayList<>());
                  save.add(itemStack);
                  this.items.put(player, save);
               }
            } else if (!itemStack.containsEnchantment(Enchantment.VANISHING_CURSE)) {
               dropItems.add(itemStack);
            }
         }
      }

      e.getDrops().clear();
      e.getDrops().addAll(dropItems);
   }

   public static void pushTo(Entity target, Location to, double multiply) {
      Vector unitVector = to.toVector().subtract(target.getLocation().toVector()).normalize();
      target.setVelocity(unitVector.multiply(multiply).add(new Vector((double)0.0F, 0.1, (double)0.0F)));
   }

   @EventHandler
   public void onUseItem(PlayerInteractEvent e) {
      final Player player = e.getPlayer();
      if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
         CustomItem customItem = this.itemAPI.getItem(player.getInventory().getItemInMainHand());
         if (customItem != null) {
            if (Cooldowns.has(player.getName(), "rightClick")) {
               return;
            }

            Cooldowns.set(player.getName(), "rightClick", (double)200.0F);
            if (customItem.isNoRightUse()) {
               e.setCancelled(true);
            }

            if (customItem.isNoStack() && player.getInventory().getItemInMainHand().getAmount() > 1) {
               player.sendMessage("§cВы не можете стакать этот предмет");
               return;
            }

            if (customItem.isEnderchestOpenAfterRightClick()) {
               player.openInventory(player.getEnderChest());
            }

            if (customItem.getDamageAfterRightClick() > 0) {
               player.damage((double)customItem.getDamageAfterRightClick());
            }

            if (customItem.isTeleportToNearHighestLocation()) {
               int attemps = 20;
               Location cl = player.getLocation().clone();
               int check = player.getLocation().getBlockY();

               while(attemps > 0) {
                  cl = cl.add((double)1.0F, (double)0.0F, (double)1.0F);
                  int y = cl.getWorld().getHighestBlockYAt(cl.getBlockX(), cl.getBlockZ()) + 1;
                  cl.setY((double)y);
                  --attemps;
                  if (y != check) {
                     break;
                  }
               }

               player.teleport(cl);
            }

            if (customItem.hasMegaJumpAfterRightClick()) {
               pushTo(player, player.getLocation().clone().add((double)0.0F, (double)customItem.getMegaJumpAfterRightClick(), (double)0.0F), customItem.getMegaJumpAfterRightClick() >= 10 ? (double)((float)customItem.getMegaJumpAfterRightClick() / 10.0F) : (double)1.0F);
               this.noDamage.add(e.getPlayer().getName());
            }

            if (Bukkit.getPluginManager().isPluginEnabled("AntiRelog") && customItem.isClearAntiRelog()) {
               try {
                  AntiRelogUtils.clearAntiRelog(player);
               } catch (Exception var8) {
                  System.out.println("EpicItems | Версия вашего антирелога не поддерживается!");
               }
            }

            if (customItem.hasCommands()) {
               e.setCancelled(true);
               this.cancelled.add(player.getName());

               for(String command : customItem.getCommands()) {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
               }

               (new BukkitRunnable() {
                  public void run() {
                     ItemsListener.this.cancelled.remove(player.getName());
                  }
               }).runTaskLater((Plugin)this.itemAPI, 10L);
            }

            if (customItem.isClearAfterRightClick()) {
               player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
            }
         }

      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      for(PotionEffect pe : event.getPlayer().getActivePotionEffects()) {
         if (pe.getDuration() > 1000000) {
            event.getPlayer().removePotionEffect(pe.getType());
         }
      }

   }
}
