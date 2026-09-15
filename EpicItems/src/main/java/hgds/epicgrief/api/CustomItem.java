package hgds.epicgrief.api;

import java.util.List;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;

public interface CustomItem {
   Material getType();

   boolean hasType();

   short getData();

   int getBrokenBlocksToDestroyPickaxe();

   boolean hasData();

   boolean hasFlags();

   List<String> getFlags();

   boolean isNoRightUse();

   boolean isEnderchestOpenAfterRightClick();

   boolean hasBrokenBlocksToDestroyPickaxe();

   boolean hasBlocksToDestroyPickaxe();

   boolean isNoKnockback();

   void setNoKnockback(boolean var1);

   void addUsedSlot(CustomItemSlot var1);

   boolean isUsedSlot(CustomItemSlot var1);

   void removeUsedSlot(CustomItemSlot var1);

   String getHeadTag();

   boolean hasHeadTag();

   boolean hasDisplayName();

   String getDisplayName();

   boolean hasLore();

   List<String> getLore();

   List<Attributes.Attribute> getAttributes();

   boolean hasAttributes();

   void setAttributes(List<Attributes.Attribute> var1);

   boolean hasEffects();

   List<String> getEffects();

   boolean isClearAntiRelog();

   List<String> getCommands();

   void setNoRightUse(boolean var1);

   boolean hasCommands();

   List<String> getEnchantments();

   boolean isNoStack();

   void setNoStack(boolean var1);

   List<String> getBlocksToDestroyPickaxe();

   void setTeleportToNearHighestLocation(boolean var1);

   boolean isTeleportToNearHighestLocation();

   int getDamageAfterRightClick();

   void setDamageAfterRightClick(int var1);

   void setMegaJumpAfterRightClick(int var1);

   boolean hasMegaJumpAfterRightClick();

   int getMegaJumpAfterRightClick();

   boolean hasEnchantments();

   int getBreakRadius();

   boolean isDefaultBreakRadius();

   boolean isDropAfterDeath();

   boolean isSaveAfterDeath();

   boolean isUnbreakable();

   boolean isDropSpawnerAfterBreak();

   boolean isRemeltingAfterBreak();

   boolean isClearAfterRightClick();

   boolean isNoRepair();

   void setDisplayName(@Nullable String var1);

   void setLore(@Nullable List<String> var1);

   void setEffects(@Nullable List<String> var1);

   void setClearAntiRelog(@Nullable boolean var1);

   void setCommands(@Nullable List<String> var1);

   void setEnchantments(@Nullable List<String> var1);

   void setBreakRadius(@Nullable int var1);

   void setDropSpawnerAfterBreak(@Nullable boolean var1);

   void setRemeltingAfterBreak(@Nullable boolean var1);

   void setDropAfterDeath(@Nullable boolean var1);

   void setSaveAfterDeath(@Nullable boolean var1);

   void setBrokenBlocksToDestroyPickaxe(@Nullable int var1);

   void setType(@Nullable Material var1);

   void setFlags(@Nullable List<String> var1);

   void setHeadTag(@Nullable String var1);

   void setNoRepair(@Nullable boolean var1);

   void setUnbreakable(@Nullable boolean var1);

   void setData(@Nullable short var1);

   void setClearAfterRightClick(@Nullable boolean var1);

   void setEnderchestOpenAfterRightClick(@Nullable boolean var1);

   void setBlocksToDestroyPickaxe(@Nullable List<String> var1);

   ItemStack getBukkit();
}
