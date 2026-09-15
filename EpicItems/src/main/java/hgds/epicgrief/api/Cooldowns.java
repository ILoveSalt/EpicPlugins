package hgds.epicgrief.api;

import java.util.HashMap;

public class Cooldowns {
   public static HashMap cooldowns = new HashMap();

   public static void set(String player, String title, double time) {
      cooldowns.put(player + title, (double)System.currentTimeMillis() + time);
   }

   public static boolean has(String player, String title) {
      return cooldowns.get(player + title) != null && (Double)cooldowns.getOrDefault(player + title, (double)0.0F) > (double)System.currentTimeMillis();
   }

   public static int get(String player, String title) {
      return (int)((Double)cooldowns.getOrDefault(player + title, (double)0.0F) - (double)System.currentTimeMillis());
   }
}
