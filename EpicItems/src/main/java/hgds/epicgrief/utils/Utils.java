package hgds.epicgrief.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import hgds.epicgrief.api.CustomItem;

public class Utils {
   public static Material resolveMaterial(String materialName) {
      if (materialName == null) {
         return null;
      } else {
         String normalized = materialName.trim();
         if (normalized.isEmpty()) {
            return null;
         } else {
            Material material = Material.matchMaterial(normalized);
            if (material != null) {
               return material;
            } else {
               material = Material.matchMaterial(normalized.toUpperCase(Locale.ROOT));
               if (material != null) {
                  return material;
               } else {
                  String lowerCase = normalized.toLowerCase(Locale.ROOT);
                  switch (lowerCase) {
                     case "mob_spawner":
                     case "spawner":
                        return Material.SPAWNER;
                     case "totem":
                     case "totem_of_undying":
                        return Material.TOTEM_OF_UNDYING;
                     case "fireball":
                     case "fire_charge":
                        return Material.FIRE_CHARGE;
                     case "skull":
                     case "player_head":
                     case "head":
                        return Material.PLAYER_HEAD;
                     default:
                        return Material.matchMaterial(normalized.replace('-', '_').toUpperCase(Locale.ROOT));
                  }
               }
            }
         }
      }
   }

   public static List<String> color(List<String> lore) {
      List<String> newLore = new ArrayList();

      for(String s : lore) {
         newLore.add(color(s));
      }

      return newLore;
   }

   public static String color(String displayName) {
      return ChatColor.translateAlternateColorCodes('&', displayName);
   }

   public static ItemStack smelt(Block block) {
      ItemStack result = null;
      Iterator<Recipe> iter = Bukkit.recipeIterator();

      while(iter.hasNext()) {
         Recipe recipe = (Recipe)iter.next();
         if (recipe instanceof FurnaceRecipe && ((FurnaceRecipe)recipe).getInput().getType() == block.getType()) {
            result = recipe.getResult();
            break;
         }
      }

      return result;
   }

   public static void breakBlocks(Location loc, CustomItem customItem, Player player) {
      int radius = customItem.getBreakRadius();
      World world = loc.getWorld();

      for(int x = loc.getBlockX() - radius; x <= loc.getBlockX() + radius; ++x) {
         for(int y = loc.getBlockY() - radius; y <= loc.getBlockY() + radius; ++y) {
            for(int z = loc.getBlockZ() - radius; z <= loc.getBlockZ() + radius; ++z) {
               Block block = world.getBlockAt(x, y, z);
               
               // Пропускаем центральный блок (который игрок уже ломает)
               if (block.getLocation().equals(loc)) {
                  continue;
               }
               
               // Пропускаем воздух и бедрок
               if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) {
                  continue;
               }
               
               // Проверяем WorldGuard права
               if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard") || WorldGuardUtils.canBuild(player, block)) {
                  // Защита от дюпа: проверяем что блок еще существует перед ломанием
                  if (block.getType() == Material.AIR) {
                     continue;
                  }
                  
                  if (block.getType() == Material.SPAWNER && customItem.isDropSpawnerAfterBreak()) {
                     Material spawnerMaterial = block.getType();
                     CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                     ItemStack itemStack = new ItemStack(spawnerMaterial);
                     BlockStateMeta itemMeta = (BlockStateMeta)itemStack.getItemMeta();
                     itemMeta.setDisplayName("§bСпавнер " + creatureSpawner.getSpawnedType().name().toLowerCase().replace("_", " "));
                     itemMeta.setBlockState(creatureSpawner.getBlock().getState());
                     itemStack.setItemMeta(itemMeta);
                     block.setType(Material.AIR);
                     block.getWorld().dropItem(block.getLocation(), itemStack);
                  } else if (customItem.isRemeltingAfterBreak()) {
                     ItemStack smeltItem = smelt(block);
                     // Дополнительная проверка перед ломанием для защиты от дюпа
                     if (block.getType() != Material.AIR) {
                        if (smeltItem != null) {
                           block.setType(Material.AIR);
                           block.getWorld().dropItem(block.getLocation(), smeltItem);
                        } else {
                           block.breakNaturally();
                        }
                     }
                  } else {
                     // Дополнительная проверка перед ломанием для защиты от дюпа
                     if (block.getType() != Material.AIR) {
                        block.breakNaturally();
                     }
                  }
               }
            }
         }
      }

   }
}
