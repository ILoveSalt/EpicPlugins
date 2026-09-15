package hgds.epicgrief.api;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.database.SQLPickaxe;

public interface ItemAPI {
   CustomItem getItem(@Nullable ItemStack var1);

   CustomItem getItem(@Nullable String var1);

   void addCustomItem(@Nullable String var1, @Nullable CustomItem var2);

   void removeCustomItem(@Nullable String var1);

   SQLPickaxe getSQLItem(@Nullable ItemStack var1);

   long buildUnicalId();

   List<CustomItem> getItems();

   void updatePlaceholder(ItemStack var1);
}
