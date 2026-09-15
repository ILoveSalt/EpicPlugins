package hgds.epicgrief;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.Attributes;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.CustomItemSlot;
import hgds.epicgrief.api.ItemAPI;
import hgds.epicgrief.api.database.SQL;
import hgds.epicgrief.api.database.impl.SQLite;
import hgds.epicgrief.api.impl.CraftCustomItem;
import hgds.epicgrief.commands.CustomItemCommand;
import hgds.epicgrief.database.ItemsSQL;
import hgds.epicgrief.database.SQLPickaxe;
import hgds.epicgrief.listeners.ItemsListener;
import hgds.epicgrief.runnables.EffectsChecker;
import hgds.epicgrief.runnables.ZalupaListener;
import hgds.epicgrief.utils.Utils;

public class EpicItems extends JavaPlugin implements ItemAPI {
   private Map<String, CustomItem> customItems;
   private ItemsSQL itemsSQL;
   private int effectCheckerTaskId = 0;
   private int effectCheckerTaskId1 = 0;

   private String formatDisplayName(String displayName) {
      return displayName.contains("(") && displayName.contains(")") ? displayName.split("§7\\(")[0] : displayName;
   }

   private void replaceDisplayName(ItemStack itemStack) {
      if (itemStack.hasItemMeta()) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            itemMeta.setDisplayName(this.formatDisplayName(itemMeta.getDisplayName()));
         }

         itemStack.setItemMeta(itemMeta);
      }
   }

   private List<Integer> formatLore(List<String> lore) {
      List<Integer> list = new ArrayList();
      if (lore == null) {
         return list;
      }

      List<String> newLore = new ArrayList();
      int i = 0;

      for(String str : lore) {
         if (str.contains("%brokenBlocks%")) {
            list.add(i);
         } else {
            newLore.add(str);
         }

         ++i;
      }

      lore.clear();
      lore.addAll(newLore);
      return list;
   }

   private List<String> getLore(List<String> oldLore) {
      List<String> lore = new ArrayList();
      if (oldLore != null) {
         for(String s : oldLore) {
            if (!s.isEmpty() && !s.equals("&r") && !s.equals("§r") && !s.equals("§7")) {
               lore.add(s.replace(" ", ""));
            }
         }
      }

      return lore;
   }

   private boolean isSimiliar(ItemStack first, ItemStack second) {
      if (first.hasItemMeta() && second.hasItemMeta()) {
         ItemMeta itemMeta = first.getItemMeta();
         ItemMeta secondMeta = second.getItemMeta();
         String displayName = itemMeta.getDisplayName() == null ? null : itemMeta.getDisplayName().replace(" ", "");
         String displayName2 = secondMeta.getDisplayName() == null ? null : secondMeta.getDisplayName().replace(" ", "");
         List<String> lore = this.getLore(itemMeta.getLore());
         List<String> lore2 = this.getLore(secondMeta.getLore());

         for(Integer integer : this.formatLore(lore)) {
            try {
               lore2.remove(integer);
            } catch (Exception var13) {
               return false;
            }
         }

         if (Objects.equals(displayName, displayName2)) {
            return Objects.equals(lore, lore2);
         }
      }

      return false;
   }

   public CustomItem getItem(@Nullable ItemStack itemStack) {
      if (itemStack != null && itemStack.getType() != Material.AIR) {
         ItemStack test = itemStack.clone();

         for(Map.Entry<String, CustomItem> customItem : this.customItems.entrySet()) {
            ItemStack bukkitCustom = ((CustomItem)customItem.getValue()).getBukkit();
            this.replaceDisplayName(bukkitCustom);
            this.replaceDisplayName(test);
            if (this.isSimiliar(bukkitCustom, test)) {
               return (CustomItem)customItem.getValue();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public CustomItem getItem(@Nullable String id) {
      return (CustomItem)this.customItems.get(id);
   }

   public void addCustomItem(@Nullable String id, @Nullable CustomItem customItem) {
      this.customItems.put(id, customItem);
   }

   public void removeCustomItem(@Nullable String id) {
      this.customItems.remove(id);
   }

   public SQLPickaxe getSQLItem(@Nullable ItemStack itemStack) {
      return this.itemsSQL.loadPickaxe(itemStack);
   }

   public long buildUnicalId() {
      return this.itemsSQL.getUnicalId();
   }

   public List<CustomItem> getItems() {
      return new ArrayList<>(this.customItems.values());
   }

   public void updatePlaceholder(ItemStack itemStack) {
      if (itemStack != null && itemStack.hasItemMeta()) {
         CustomItem customItem = this.getItem(itemStack);
         if (customItem == null) {
            return;
         }

         if (!customItem.hasBrokenBlocksToDestroyPickaxe()) {
            return;
         }

         SQLPickaxe sqlPickaxe = this.getSQLItem(itemStack);
         if (sqlPickaxe == null) {
            return;
         }

         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            String id = itemMeta.getDisplayName().split(" §7\\(")[1];
            if (customItem.hasDisplayName()) {
               itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customItem.getDisplayName().replace("%brokenBlocks%", String.valueOf(customItem.getBrokenBlocksToDestroyPickaxe() - sqlPickaxe.getBrokenBlocks())) + " §7(" + id));
            }
         }

         if (itemMeta.hasLore()) {
            List<String> lore = new ArrayList();

            for(String s : customItem.getLore()) {
               lore.add(ChatColor.translateAlternateColorCodes('&', s.replace("%brokenBlocks%", String.valueOf(customItem.getBrokenBlocksToDestroyPickaxe() - sqlPickaxe.getBrokenBlocks()))));
            }

            itemMeta.setLore(lore);
         }

         itemStack.setItemMeta(itemMeta);
      }

   }

   private void loadItems() {
      if (this.customItems == null) {
         this.customItems = new ConcurrentHashMap<>();
      }

      this.customItems.clear();

      ConfigurationSection itemsSection = this.getConfig().getConfigurationSection("items");
      if (itemsSection == null) {
         if (!this.getConfig().isList("items") || !this.getConfig().getStringList("items").isEmpty()) {
            this.getLogger().warning("Config section 'items' is missing or is not a section. No custom items loaded.");
         }
         return;
      }

      for(String items : itemsSection.getKeys(false)) {
         ConfigurationSection itemConfiguration = itemsSection.getConfigurationSection(items);
         if (itemConfiguration == null) {
            this.getLogger().warning("Item '" + items + "' is not a configuration section. Skipping it.");
            continue;
         }
         CustomItem customItem = new CraftCustomItem();
         if (itemConfiguration.contains("type")) {
            String type = itemConfiguration.getString("type");
            if (type != null && type.toUpperCase().startsWith("HEAD:")) {
               customItem.setHeadTag(type.split(":")[1]);
            } else {
               Material material = Utils.resolveMaterial(type);
               if (material != null) {
                  customItem.setType(material);
               }
            }
         }

         if (itemConfiguration.contains("displayName")) {
            customItem.setDisplayName(itemConfiguration.getString("displayName"));
         }

         if (itemConfiguration.contains("lore")) {
            customItem.setLore(itemConfiguration.getStringList("lore"));
         }

         if (itemConfiguration.contains("effects")) {
            customItem.setEffects(itemConfiguration.getStringList("effects"));
         }

         if (itemConfiguration.contains("commands")) {
            customItem.setCommands(itemConfiguration.getStringList("commands"));
         }

         if (itemConfiguration.contains("dropAfterDeath")) {
            customItem.setDropAfterDeath(itemConfiguration.getBoolean("dropAfterDeath"));
         }

         if (itemConfiguration.contains("saveAfterDeath")) {
            customItem.setSaveAfterDeath(itemConfiguration.getBoolean("saveAfterDeath"));
         }

         if (itemConfiguration.contains("data")) {
            customItem.setData((short)itemConfiguration.getInt("data"));
         }

         ConfigurationSection attributesSection = itemConfiguration.getConfigurationSection("attributes");
         if (attributesSection != null) {
            List<Attributes.Attribute> attributes = new ArrayList<>();

            for(String attribute : attributesSection.getKeys(false)) {
               ConfigurationSection attributeSection = attributesSection.getConfigurationSection(attribute);
               if (attributeSection == null) {
                  continue;
               }
               Attributes.Attribute.Builder builder = Attributes.Attribute.newBuilder().type(Attributes.AttributeType.fromId(attributeSection.getString("minecraftId"))).name(attributeSection.getString("name")).amount(attributeSection.getDouble("amount")).operation(Attributes.Operation.fromId(attributeSection.getInt("operationId")));
               if (attributeSection.getString("slot", (String)null) != null) {
                  builder = builder.slot(Attributes.Slot.fromId(attributeSection.getString("slot", (String)null)));
               }

               attributes.add(builder.build());
            }

            customItem.setAttributes(attributes);
         }

         if (itemConfiguration.contains("noKnockback")) {
            customItem.setNoKnockback(itemConfiguration.getBoolean("noKnockback"));
         }

         if (itemConfiguration.contains("clearAntiRelog")) {
            customItem.setClearAntiRelog(itemConfiguration.getBoolean("clearAntiRelog"));
         }

         if (itemConfiguration.contains("clearAfterRightClick")) {
            customItem.setClearAfterRightClick(itemConfiguration.getBoolean("clearAfterRightClick"));
         }

         if (itemConfiguration.contains("breakRadius")) {
            customItem.setBreakRadius(itemConfiguration.getInt("breakRadius"));
         }

         if (itemConfiguration.contains("enchantments")) {
            customItem.setEnchantments(itemConfiguration.getStringList("enchantments"));
         }

         if (itemConfiguration.contains("enderchestOpenAfterRightClick")) {
            customItem.setEnderchestOpenAfterRightClick(itemConfiguration.getBoolean("enderchestOpenAfterRightClick"));
         }

         if (itemConfiguration.contains("flags")) {
            customItem.setFlags(itemConfiguration.getStringList("flags"));
         }

         if (itemConfiguration.contains("unbreakable")) {
            customItem.setUnbreakable(itemConfiguration.getBoolean("unbreakable"));
         }

         if (itemConfiguration.contains("spawnerDropAfterBreak")) {
            customItem.setDropSpawnerAfterBreak(itemConfiguration.getBoolean("spawnerDropAfterBreak"));
         }

         if (itemConfiguration.contains("remeltingAfterBreak")) {
            customItem.setRemeltingAfterBreak(itemConfiguration.getBoolean("remeltingAfterBreak"));
         }

         if (itemConfiguration.contains("noRepair")) {
            customItem.setNoRepair(itemConfiguration.getBoolean("noRepair"));
         }

         if (itemConfiguration.contains("brokenBlocksToDestroyPickaxe")) {
            customItem.setBrokenBlocksToDestroyPickaxe(itemConfiguration.getInt("brokenBlocksToDestroyPickaxe"));
         }

         if (itemConfiguration.contains("blocksToDestroyPickaxe")) {
            customItem.setBlocksToDestroyPickaxe(itemConfiguration.getStringList("blocksToDestroyPickaxe"));
         }

         if (itemConfiguration.contains("megaJumpAfterRightClick")) {
            customItem.setMegaJumpAfterRightClick(itemConfiguration.getInt("megaJumpAfterRightClick"));
         }

         if (itemConfiguration.contains("damageAfterRightClick")) {
            customItem.setDamageAfterRightClick(itemConfiguration.getInt("damageAfterRightClick"));
         }

         if (itemConfiguration.contains("teleportToNearHighestLocation")) {
            customItem.setTeleportToNearHighestLocation(itemConfiguration.getBoolean("teleportToNearHighestLocation"));
         }

         if (itemConfiguration.contains("noStack")) {
            customItem.setNoStack(itemConfiguration.getBoolean("noStack"));
         }

         if (itemConfiguration.contains("usedSlots")) {
            for(String usedSlots : itemConfiguration.getStringList("usedSlots")) {
               customItem.addUsedSlot(CustomItemSlot.getByName(usedSlots));
            }
         }

         if (itemConfiguration.contains("noRightUse")) {
            customItem.setNoRightUse(itemConfiguration.getBoolean("noRightUse"));
         }

         this.customItems.put(items, customItem);
      }

   }

   public void registerDatabase() {
      SQL sql = new SQLite(new File(this.getDataFolder(), "database.db"));
      sql.connect();
      (this.itemsSQL = new ItemsSQL()).load(sql);
   }

   public void unregisterDatabase() {
      if (this.itemsSQL == null) {
         return;
      }

      this.itemsSQL.save();
      this.itemsSQL.close();
      this.itemsSQL = null;
   }

   public void reload() {
      this.reloadConfig();
      this.loadItems();
   }

   public void onEnable() {
      this.saveDefaultConfig();
      this.reload();
      this.effectCheckerTaskId = (new EffectsChecker()).runTaskTimer(this, 20L, 20L).getTaskId();
      this.effectCheckerTaskId1 = (new ZalupaListener()).runTaskTimer(this, 1L, 1L).getTaskId();
      this.getServer().getPluginManager().registerEvents(new ItemsListener(), this);
      this.getCommand("customitems").setExecutor(new CustomItemCommand());
      this.registerDatabase();
   }

   public void onDisable() {
      Bukkit.getScheduler().cancelTask(this.effectCheckerTaskId);
      Bukkit.getScheduler().cancelTask(this.effectCheckerTaskId1);
      this.unregisterDatabase();
   }
}
