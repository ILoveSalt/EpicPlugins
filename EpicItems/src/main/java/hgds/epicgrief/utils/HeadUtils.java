package hgds.epicgrief.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class HeadUtils {
   public static ItemStack getHead(String value) {
      ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
      if (skullMeta == null) {
         return itemStack;
      } else {
         PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
         profile.setProperty(new ProfileProperty("textures", value.replace("<base64>", "")));
         skullMeta.setPlayerProfile(profile);
         itemStack.setItemMeta(skullMeta);
         return itemStack;
      }
   }
}
