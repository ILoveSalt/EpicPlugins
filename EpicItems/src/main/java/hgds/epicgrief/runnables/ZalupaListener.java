package hgds.epicgrief.runnables;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.ItemAPI;

public class ZalupaListener extends BukkitRunnable {
   private ItemAPI itemAPI = (ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems");

   public void run() {
      for(Player player : Bukkit.getOnlinePlayers()) {
         this.removeFromHelmet(player);
         this.removeFromSlot(player);
         this.removeFromHand(player);
      }

   }

   private void removeFromHelmet(Player player) {
      if (player.getInventory().getItemInMainHand() != null) {
         CustomItem item = this.itemAPI.getItem(player.getInventory().getItemInMainHand());
         if (item != null) {
            if (item.hasAttributes()) {
               if (player.getInventory().getHelmet() != null) {
                  CustomItem helmet = this.itemAPI.getItem(player.getInventory().getHelmet());
                  if (helmet != null) {
                     if (helmet.hasAttributes()) {
                        if (helmet.getHeadTag().equals(item.getHeadTag())) {
                           player.getInventory().addItem(new ItemStack[]{helmet.getBukkit()});
                           player.getInventory().setHelmet((ItemStack)null);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void removeFromSlot(Player player) {
      if (player.getInventory().getItemInMainHand() != null) {
         CustomItem item = this.itemAPI.getItem(player.getInventory().getItemInMainHand());
         if (item != null) {
            if (item.hasAttributes()) {
               if (player.getInventory().getItemInOffHand() != null) {
                  CustomItem offHand = this.itemAPI.getItem(player.getInventory().getItemInOffHand());
                  if (offHand != null) {
                     if (offHand.hasAttributes()) {
                        if (offHand.getHeadTag().equals(item.getHeadTag())) {
                           player.getInventory().addItem(new ItemStack[]{offHand.getBukkit()});
                           player.getInventory().setItemInOffHand((ItemStack)null);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void removeFromHand(Player player) {
      if (player.getInventory().getItemInOffHand() != null) {
         CustomItem item = this.itemAPI.getItem(player.getInventory().getItemInOffHand());
         if (item != null) {
            if (item.hasAttributes()) {
               if (player.getInventory().getHelmet() != null) {
                  CustomItem helmet = this.itemAPI.getItem(player.getInventory().getHelmet());
                  if (helmet != null) {
                     if (helmet.hasAttributes()) {
                        if (helmet.getHeadTag().equals(item.getHeadTag())) {
                           player.getInventory().addItem(new ItemStack[]{helmet.getBukkit()});
                           player.getInventory().setItemInOffHand((ItemStack)null);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
