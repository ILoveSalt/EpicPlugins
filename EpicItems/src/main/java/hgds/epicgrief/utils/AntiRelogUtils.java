package hgds.epicgrief.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AntiRelogUtils {
   public static void clearAntiRelog(Player player) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("AntiRelog");
      if (plugin == null) {
         return;
      }

      try {
         Object pvpManager = plugin.getClass().getMethod("getPvpManager").invoke(plugin);
         if (pvpManager != null) {
            pvpManager.getClass().getMethod("stopPvPSilent", Player.class).invoke(pvpManager, player);
         }
      } catch (ReflectiveOperationException ignored) {
      }

   }
}
