package hgds.epicgrief.runnables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.CustomItemSlot;
import hgds.epicgrief.api.ItemAPI;

public class EffectsChecker extends BukkitRunnable {
   public void run() {
      ItemAPI itemAPI = (ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems");

      for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
         List<ItemStack> playerActiveItems = new ArrayList<>();
         if (onlinePlayer.getInventory().getItemInMainHand() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getItemInMainHand());
         }

         if (onlinePlayer.getInventory().getItemInOffHand() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getItemInOffHand());
         }

         if (onlinePlayer.getInventory().getHelmet() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getHelmet());
         }

         if (onlinePlayer.getInventory().getChestplate() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getChestplate());
         }

         if (onlinePlayer.getInventory().getLeggings() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getLeggings());
         }

         if (onlinePlayer.getInventory().getBoots() != null) {
            playerActiveItems.add(onlinePlayer.getInventory().getBoots());
         }

         Map<PotionEffectType, Integer> potionEffectTypes = new HashMap<>();

         for(ItemStack playerActiveItem : playerActiveItems) {
            CustomItem customItem = itemAPI.getItem(playerActiveItem);
            if (customItem != null && customItem.hasEffects() && (customItem.isUsedSlot(CustomItemSlot.RIGHT_HAND) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getItemInMainHand())) && (customItem.isUsedSlot(CustomItemSlot.LEFT_HAND) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getItemInOffHand())) && (customItem.isUsedSlot(CustomItemSlot.HEAD) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getHelmet())) && (customItem.isUsedSlot(CustomItemSlot.CHEST_PLATE) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getChestplate())) && (customItem.isUsedSlot(CustomItemSlot.LEGGINGS) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getLeggings())) && (customItem.isUsedSlot(CustomItemSlot.BOOTS) || !playerActiveItem.isSimilar(onlinePlayer.getInventory().getBoots()))) {
               for(String effect : customItem.getEffects()) {
                  try {
                     PotionEffectType potionEffectType = PotionEffectType.getByName(effect.split(":")[0]);
                     int level = Integer.parseInt(effect.split(":")[1]);
                     if (!potionEffectTypes.containsKey(potionEffectType) || (Integer)potionEffectTypes.get(potionEffectType) <= level) {
                        potionEffectTypes.put(potionEffectType, level);
                     }
                  } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex2) {
                     ((RuntimeException)ex2).printStackTrace();
                  }
               }
            }
         }

         for(Map.Entry<PotionEffectType, Integer> potionEffectTypeIntegerEntry : potionEffectTypes.entrySet()) {
            PotionEffect oldEffect = onlinePlayer.getPotionEffect((PotionEffectType)potionEffectTypeIntegerEntry.getKey());
            boolean force = oldEffect != null && oldEffect.getAmplifier() != (Integer)potionEffectTypeIntegerEntry.getValue() && oldEffect.getDuration() > 100000 && oldEffect.getDuration() < 999999;
            onlinePlayer.addPotionEffect(new PotionEffect((PotionEffectType)potionEffectTypeIntegerEntry.getKey(), 999999, (Integer)potionEffectTypeIntegerEntry.getValue()), force);
         }

         for(PotionEffect activePotionEffect : onlinePlayer.getActivePotionEffects()) {
            if (activePotionEffect.getDuration() > 100000 && activePotionEffect.getDuration() < 999999 && !potionEffectTypes.containsKey(activePotionEffect.getType())) {
               onlinePlayer.removePotionEffect(activePotionEffect.getType());
            }
         }
      }

   }
}
