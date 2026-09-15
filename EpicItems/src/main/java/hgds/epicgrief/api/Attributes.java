package hgds.epicgrief.api;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Attributes {
   private final ItemStack stack;
   private final List<Attribute> attributes = new ArrayList();

   public Attributes(ItemStack stack) {
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public int size() {
      return this.attributes.size();
   }

   public void add(Attribute attribute) {
      if (attribute != null && attribute.getAttributeType() != null && attribute.getAttributeType().getBukkitAttribute() != null) {
         ItemMeta itemMeta = this.stack.getItemMeta();
         if (itemMeta != null) {
            itemMeta.addAttributeModifier(attribute.getAttributeType().getBukkitAttribute(), attribute.toBukkitModifier());
            this.stack.setItemMeta(itemMeta);
            this.attributes.add(attribute);
         }
      }

   }

   public boolean remove(Attribute attribute) {
      if (attribute == null) {
         return false;
      } else {
         ItemMeta itemMeta = this.stack.getItemMeta();
         if (itemMeta == null) {
            return false;
         } else if (attribute.getAttributeType() != null && attribute.getAttributeType().getBukkitAttribute() != null) {
            boolean removed = itemMeta.removeAttributeModifier(attribute.getAttributeType().getBukkitAttribute(), attribute.toBukkitModifier());
            if (removed) {
               this.stack.setItemMeta(itemMeta);
               this.attributes.removeIf((existing) -> Objects.equals(existing.getUUID(), attribute.getUUID()));
            }

            return removed;
         } else {
            return false;
         }
      }
   }

   public void clear() {
      ItemMeta itemMeta = this.stack.getItemMeta();
      if (itemMeta != null) {
         Multimap<org.bukkit.attribute.Attribute, AttributeModifier> modifiers = itemMeta.getAttributeModifiers();
         if (modifiers != null) {
            for(org.bukkit.attribute.Attribute attribute : new ArrayList<>(modifiers.keySet())) {
               itemMeta.removeAttributeModifier(attribute);
            }
         }

         this.stack.setItemMeta(itemMeta);
      }

      this.attributes.clear();
   }

   public Attribute get(int index) {
      return (Attribute)this.attributes.get(index);
   }

   public Iterable<Attribute> values() {
      return Collections.unmodifiableList(this.attributes);
   }

   public static enum Operation {
      ADD_NUMBER(0, AttributeModifier.Operation.ADD_NUMBER),
      MULTIPLY_PERCENTAGE(1, AttributeModifier.Operation.ADD_SCALAR),
      ADD_PERCENTAGE(2, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

      private final int id;
      private final AttributeModifier.Operation bukkitOperation;

      private Operation(int id, AttributeModifier.Operation bukkitOperation) {
         this.id = id;
         this.bukkitOperation = bukkitOperation;
      }

      public int getId() {
         return this.id;
      }

      public AttributeModifier.Operation getBukkitOperation() {
         return this.bukkitOperation;
      }

      public static Operation fromId(int id) {
         for(Operation op : values()) {
            if (op.getId() == id) {
               return op;
            }
         }

         throw new IllegalArgumentException("Corrupt operation ID " + id + " detected.");
      }
   }

   public static class AttributeType {
      private static final Map<String, AttributeType> LOOKUP = new ConcurrentHashMap();
      public static final AttributeType GENERIC_MAX_HEALTH = register(new AttributeType("generic.maxHealth", org.bukkit.attribute.Attribute.MAX_HEALTH));
      public static final AttributeType GENERIC_FOLLOW_RANGE = register(new AttributeType("generic.followRange", org.bukkit.attribute.Attribute.FOLLOW_RANGE));
      public static final AttributeType GENERIC_ATTACK_DAMAGE = register(new AttributeType("generic.attackDamage", org.bukkit.attribute.Attribute.ATTACK_DAMAGE));
      public static final AttributeType GENERIC_MOVEMENT_SPEED = register(new AttributeType("generic.movementSpeed", org.bukkit.attribute.Attribute.MOVEMENT_SPEED));
      public static final AttributeType GENERIC_KNOCKBACK_RESISTANCE = register(new AttributeType("generic.knockbackResistance", org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE));
      public static final AttributeType GENERIC_ARMOR = register(new AttributeType("generic.armor", org.bukkit.attribute.Attribute.ARMOR));
      public static final AttributeType GENERIC_ARMOR_TOUGHNESS = register(new AttributeType("generic.armorToughness", org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS));
      public static final AttributeType GENERIC_ATTACK_SPEED = register(new AttributeType("generic.attackSpeed", org.bukkit.attribute.Attribute.ATTACK_SPEED));
      public static final AttributeType GENERIC_LUCK = register(new AttributeType("generic.luck", org.bukkit.attribute.Attribute.LUCK));
      private final String minecraftId;
      private final org.bukkit.attribute.Attribute bukkitAttribute;

      public AttributeType(String minecraftId, org.bukkit.attribute.Attribute bukkitAttribute) {
         this.minecraftId = minecraftId;
         this.bukkitAttribute = bukkitAttribute;
      }

      private static AttributeType register(AttributeType attributeType) {
         LOOKUP.put(attributeType.minecraftId, attributeType);
         return attributeType;
      }

      public String getMinecraftId() {
         return this.minecraftId;
      }

      public org.bukkit.attribute.Attribute getBukkitAttribute() {
         return this.bukkitAttribute;
      }

      public static AttributeType fromId(String minecraftId) {
         if (minecraftId == null) {
            return null;
         } else {
            AttributeType attributeType = (AttributeType)LOOKUP.get(minecraftId);
            if (attributeType != null) {
               return attributeType;
            } else {
               return (AttributeType)LOOKUP.get(minecraftId.toLowerCase(Locale.ROOT));
            }
         }
      }

      public static Iterable values() {
         return LOOKUP.values();
      }
   }

   public static class Slot {
      public static final Slot MAIN_HAND = new Slot("mainhand", EquipmentSlotGroup.MAINHAND);
      public static final Slot OFF_HAND = new Slot("offhand", EquipmentSlotGroup.OFFHAND);
      public static final Slot HEAD = new Slot("head", EquipmentSlotGroup.HEAD);
      public static final Slot CHEST = new Slot("chest", EquipmentSlotGroup.CHEST);
      public static final Slot LEGS = new Slot("legs", EquipmentSlotGroup.LEGS);
      public static final Slot BOOTS = new Slot("boots", EquipmentSlotGroup.FEET);
      private final String id;
      private final EquipmentSlotGroup slotGroup;

      public Slot(String id, EquipmentSlotGroup slotGroup) {
         this.id = id;
         this.slotGroup = slotGroup;
      }

      public String getId() {
         return this.id;
      }

      public EquipmentSlotGroup getSlotGroup() {
         return this.slotGroup;
      }

      public static Slot fromId(String id) {
         if (id == null) {
            return OFF_HAND;
         } else if (id.equalsIgnoreCase(MAIN_HAND.getId())) {
            return MAIN_HAND;
         } else if (id.equalsIgnoreCase(OFF_HAND.getId())) {
            return OFF_HAND;
         } else if (id.equalsIgnoreCase(HEAD.getId())) {
            return HEAD;
         } else if (id.equalsIgnoreCase(CHEST.getId())) {
            return CHEST;
         } else if (id.equalsIgnoreCase(LEGS.getId())) {
            return LEGS;
         } else {
            return id.equalsIgnoreCase(BOOTS.getId()) ? BOOTS : OFF_HAND;
         }
      }
   }

   public static class Attribute {
      private double amount;
      private Operation operation;
      private AttributeType type;
      private String name;
      private UUID uuid;
      private Slot slot;

      private Attribute(Builder builder) {
         this.amount = builder.amount;
         this.operation = builder.operation;
         this.type = builder.type;
         this.name = builder.name;
         this.uuid = builder.uuid;
         this.slot = builder.slot;
      }

      public void setSlot(Slot slot) {
         this.slot = slot;
      }

      public Slot getSlot() {
         return this.slot;
      }

      public double getAmount() {
         return this.amount;
      }

      public void setAmount(double amount) {
         this.amount = amount;
      }

      public Operation getOperation() {
         return this.operation;
      }

      public void setOperation(Operation operation) {
         this.operation = operation;
      }

      public AttributeType getAttributeType() {
         return this.type;
      }

      public void setAttributeType(AttributeType type) {
         this.type = type;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public UUID getUUID() {
         return this.uuid;
      }

      public void setUUID(UUID id) {
         this.uuid = id;
      }

      public AttributeModifier toBukkitModifier() {
         return new AttributeModifier(this.uuid, this.name, this.amount, this.operation.getBukkitOperation(), this.slot != null ? this.slot.getSlotGroup() : EquipmentSlotGroup.ANY);
      }

      public static Builder newBuilder() {
         return (new Builder()).uuid(UUID.randomUUID()).operation(Attributes.Operation.ADD_NUMBER);
      }

      public static class Builder {
         private double amount;
         private Operation operation;
         private AttributeType type;
         private String name;
         private UUID uuid;
         private Slot slot;

         private Builder() {
            this.operation = Attributes.Operation.ADD_NUMBER;
         }

         public Builder slot(Slot slot) {
            this.slot = slot;
            return this;
         }

         public Builder amount(double amount) {
            this.amount = amount;
            return this;
         }

         public Builder operation(Operation operation) {
            this.operation = operation;
            return this;
         }

         public Builder type(AttributeType type) {
            this.type = type;
            return this;
         }

         public Builder name(String name) {
            this.name = name;
            return this;
         }

         public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
         }

         public Attribute build() {
            return new Attribute(this);
         }
      }
   }
}
