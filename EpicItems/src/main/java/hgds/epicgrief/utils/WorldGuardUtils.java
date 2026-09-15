package hgds.epicgrief.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardUtils {
   private static WorldGuardPlugin worldGuard = null;

   static {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
      if (plugin instanceof WorldGuardPlugin) {
         worldGuard = (WorldGuardPlugin) plugin;
      }
   }

   public static boolean canBuild(Player player, Block block) {
      if (worldGuard == null) {
         return true;
      }

      try {
         RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
         ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));
         
         // Проверяем, имеет ли игрок право строить в этом регионе
         return regions.testState(worldGuard.wrapPlayer(player), Flags.BUILD);
      } catch (Exception e) {
         // Если WorldGuard недоступен или произошла ошибка, разрешаем действие
         return true;
      }
   }
}
