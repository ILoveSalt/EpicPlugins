package hgds.epicgrief.api.impl;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import hgds.epicgrief.api.Attributes;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.CustomItemSlot;
import hgds.epicgrief.api.ItemAPI;
import hgds.epicgrief.utils.HeadUtils;
import hgds.epicgrief.utils.Utils;

public class CraftCustomItem implements CustomItem {
   private Material type;
   private String displayName;
   private List<String> lore;
   private List<String> effects;
   private boolean clearAntiRelog;
   private List<Attributes.Attribute> attributes;
   private List<String> commands;
   private List<CustomItemSlot> usedSlots;
   private List<String> enchantments;
   private int breakRadius;
   private boolean dropAfterDeath;
   private int brokenBlocksToDestroyPickaxe;
   private boolean saveAfterDeath;
   private boolean noRepair;
   private String headTag;
   private short data;
   private boolean noKnockback;
   private boolean clearAfterRightClick;
   private boolean enderchestOpenAfterRightClick;
   private List<String> flags;
   private List<String> blocksToDestroyPickaxe;
   private boolean unbreakable;
   private boolean dropSpawnerAfterBreak;
   private int damageAfterRightClick;
   private boolean teleportToNearHighestLocation;
   private boolean remeltingAfterBreak;
   private boolean noStack;
   private boolean noRightUse;
   private int megaJump;

   public CraftCustomItem() {
      this((Material)null);
   }

   public CraftCustomItem(Material type) {
      this.type = type;
      this.megaJump = -1;
      this.clearAntiRelog = false;
      this.breakRadius = -1;
      this.dropAfterDeath = true;
      this.noRightUse = true;
      this.saveAfterDeath = false;
      this.usedSlots = new ArrayList();
      this.brokenBlocksToDestroyPickaxe = -1;
   }

   public Material getType() {
      return this.type;
   }

   public boolean hasType() {
      return this.type != null;
   }

   public short getData() {
      return this.data;
   }

   public int getBrokenBlocksToDestroyPickaxe() {
      return this.brokenBlocksToDestroyPickaxe;
   }

   public boolean hasData() {
      return this.data != -1;
   }

   public boolean hasFlags() {
      return this.flags != null;
   }

   public List<String> getFlags() {
      return this.flags;
   }

   public boolean isNoRightUse() {
      return this.noRightUse;
   }

   public boolean isEnderchestOpenAfterRightClick() {
      return this.enderchestOpenAfterRightClick;
   }

   public boolean hasBrokenBlocksToDestroyPickaxe() {
      return this.brokenBlocksToDestroyPickaxe != -1;
   }

   public boolean hasBlocksToDestroyPickaxe() {
      return this.blocksToDestroyPickaxe != null;
   }

   public boolean isNoKnockback() {
      return this.noKnockback;
   }

   public void setNoKnockback(boolean noKnockback) {
      this.noKnockback = noKnockback;
   }

   public void addUsedSlot(CustomItemSlot customItemSlot) {
      this.usedSlots.add(customItemSlot);
   }

   public boolean isUsedSlot(CustomItemSlot customItemSlot) {
      return this.usedSlots.size() == 0 || this.usedSlots.contains(customItemSlot);
   }

   public void removeUsedSlot(CustomItemSlot customItemSlot) {
      this.usedSlots.remove(customItemSlot);
   }

   public String getHeadTag() {
      return this.headTag;
   }

   public boolean hasHeadTag() {
      return this.headTag != null;
   }

   public boolean hasDisplayName() {
      return this.displayName != null;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean hasLore() {
      return this.lore != null;
   }

   public List<String> getLore() {
      return this.lore;
   }

   public List<Attributes.Attribute> getAttributes() {
      return this.attributes;
   }

   public boolean hasAttributes() {
      return this.attributes != null && this.attributes.size() > 0;
   }

   public void setAttributes(List<Attributes.Attribute> attributes) {
      this.attributes = attributes;
   }

   public boolean hasEffects() {
      return this.effects != null;
   }

   public List<String> getEffects() {
      return this.effects;
   }

   public boolean isClearAntiRelog() {
      return this.clearAntiRelog;
   }

   public List<String> getCommands() {
      return this.commands;
   }

   public void setNoRightUse(boolean noRightUses) {
      this.noRightUse = noRightUses;
   }

   public boolean hasCommands() {
      return this.commands != null;
   }

   public List<String> getEnchantments() {
      return this.enchantments;
   }

   public boolean isNoStack() {
      return this.noStack;
   }

   public void setNoStack(boolean noStack) {
      this.noStack = noStack;
   }

   public List<String> getBlocksToDestroyPickaxe() {
      return this.blocksToDestroyPickaxe;
   }

   public void setTeleportToNearHighestLocation(boolean teleportToNearHighestLocation) {
      this.teleportToNearHighestLocation = teleportToNearHighestLocation;
   }

   public boolean isTeleportToNearHighestLocation() {
      return this.teleportToNearHighestLocation;
   }

   public int getDamageAfterRightClick() {
      return this.damageAfterRightClick;
   }

   public void setDamageAfterRightClick(int damageAfterRightClick) {
      this.damageAfterRightClick = damageAfterRightClick;
   }

   public void setMegaJumpAfterRightClick(int megaJump) {
      this.megaJump = megaJump;
   }

   public boolean hasMegaJumpAfterRightClick() {
      return this.megaJump != -1;
   }

   public int getMegaJumpAfterRightClick() {
      return this.megaJump;
   }

   public boolean hasEnchantments() {
      return this.enchantments != null;
   }

   public int getBreakRadius() {
      return this.breakRadius;
   }

   public boolean isDefaultBreakRadius() {
      return this.breakRadius == -1;
   }

   public boolean isDropAfterDeath() {
      return this.dropAfterDeath;
   }

   public boolean isSaveAfterDeath() {
      return this.saveAfterDeath;
   }

   public boolean isUnbreakable() {
      return this.unbreakable;
   }

   public boolean isDropSpawnerAfterBreak() {
      return this.dropSpawnerAfterBreak;
   }

   public boolean isRemeltingAfterBreak() {
      return this.remeltingAfterBreak;
   }

   public boolean isClearAfterRightClick() {
      return this.clearAfterRightClick;
   }

   public boolean isNoRepair() {
      return this.noRepair;
   }

   public void setDisplayName(@Nullable String displayName) {
      this.displayName = displayName;
   }

   public void setLore(@Nullable List<String> lore) {
      this.lore = lore;
   }

   public void setEffects(@Nullable List<String> effects) {
      this.effects = effects;
   }

   public void setClearAntiRelog(@Nullable boolean clearAntiRelog) {
      this.clearAntiRelog = clearAntiRelog;
   }

   public void setCommands(@Nullable List<String> commands) {
      this.commands = commands;
   }

   public void setEnchantments(@Nullable List<String> enchantments) {
      this.enchantments = enchantments;
   }

   public void setBreakRadius(@Nullable int breakRadius) {
      this.breakRadius = breakRadius;
   }

   public void setDropSpawnerAfterBreak(@Nullable boolean dropSpawnerAfterBreak) {
      this.dropSpawnerAfterBreak = dropSpawnerAfterBreak;
   }

   public void setRemeltingAfterBreak(@Nullable boolean remeltingAfterBreak) {
      this.remeltingAfterBreak = remeltingAfterBreak;
   }

   public void setDropAfterDeath(@Nullable boolean dropAfterDeath) {
      this.dropAfterDeath = dropAfterDeath;
   }

   public void setSaveAfterDeath(@Nullable boolean saveAfterDeath) {
      this.saveAfterDeath = saveAfterDeath;
   }

   public void setBrokenBlocksToDestroyPickaxe(@Nullable int brokenBlocksToDestroyPickaxe) {
      this.brokenBlocksToDestroyPickaxe = brokenBlocksToDestroyPickaxe;
   }

   public void setType(@Nullable Material type) {
      this.type = type;
   }

   public void setFlags(@Nullable List<String> flags) {
      this.flags = flags;
   }

   public void setHeadTag(@Nullable String headTag) {
      this.headTag = headTag;
   }

   public void setNoRepair(@Nullable boolean noRepair) {
      this.noRepair = noRepair;
   }

   public void setUnbreakable(@Nullable boolean unbreakable) {
      this.unbreakable = unbreakable;
   }

   public void setData(@Nullable short data) {
      this.data = data;
   }

   public void setClearAfterRightClick(@Nullable boolean clearAfterRightClick) {
      this.clearAfterRightClick = clearAfterRightClick;
   }

   public void setEnderchestOpenAfterRightClick(@Nullable boolean enderchestOpenAfterRightClick) {
      this.enderchestOpenAfterRightClick = enderchestOpenAfterRightClick;
   }

   public void setBlocksToDestroyPickaxe(@Nullable List<String> blocksToDestroyPickaxe) {
      this.blocksToDestroyPickaxe = blocksToDestroyPickaxe;
   }

   public ItemStack getBukkit() {
      ItemStack itemStack;
      if (this.hasHeadTag()) {
         itemStack = HeadUtils.getHead(this.getHeadTag());
      } else {
         itemStack = new ItemStack(this.hasType() ? this.getType() : Material.BEDROCK);
      }

      if (this.hasData()) {
         itemStack.setDurability(this.getData());
      }

      ItemMeta itemMeta = itemStack.getItemMeta();
      if (this.hasDisplayName()) {
         itemMeta.setDisplayName(Utils.color(this.getDisplayName()));
      }

      if (this.hasBrokenBlocksToDestroyPickaxe()) {
         String displayName = itemMeta.getDisplayName();
         if (displayName == null) {
            displayName = "§7(" + ((ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems")).buildUnicalId() + ")";
         } else {
            displayName = displayName + " §7(" + ((ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems")).buildUnicalId() + ")";
         }

         itemMeta.setDisplayName(displayName);
      }

      if (this.hasLore()) {
         itemMeta.setLore(Utils.color(this.getLore()));
      }

      itemMeta.setUnbreakable(this.isUnbreakable());
      if (this.hasFlags()) {
         for(String flag : this.getFlags()) {
            ItemFlag itemFlag;
            try {
               itemFlag = ItemFlag.valueOf(flag.toUpperCase());
            } catch (Exception var9) {
               continue;
            }

            itemMeta.addItemFlags(new ItemFlag[]{itemFlag});
         }
      }

      itemStack.setItemMeta(itemMeta);
      if (this.hasEnchantments()) {
         for(String enchantmentStr : this.getEnchantments()) {
            Enchantment enchantment = Enchantment.getByName(enchantmentStr.split(":")[0].toUpperCase());
            if (enchantment != null) {
               int level;
               try {
                  level = Integer.parseInt(enchantmentStr.split(":")[1]);
               } catch (NumberFormatException var8) {
                  continue;
               }

               itemStack.addUnsafeEnchantment(enchantment, level);
            }
         }
      }

      if (this.hasAttributes()) {
         Attributes attributes = new Attributes(itemStack);
         this.getAttributes().forEach(attributes::add);
         itemStack = attributes.getStack();
      }

      return itemStack;
   }
}
