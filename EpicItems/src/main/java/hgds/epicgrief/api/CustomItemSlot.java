package hgds.epicgrief.api;

public enum CustomItemSlot {
   HEAD,
   CHEST_PLATE,
   LEGGINGS,
   BOOTS,
   LEFT_HAND,
   RIGHT_HAND;

   public static CustomItemSlot getByName(String name) {
      for(CustomItemSlot value : values()) {
         if (value.name().equalsIgnoreCase(name)) {
            return value;
         }
      }

      return RIGHT_HAND;
   }
}
