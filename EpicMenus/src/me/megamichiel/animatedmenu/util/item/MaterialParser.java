package me.megamichiel.animatedmenu.util.item;

import me.megamichiel.animationlib.bukkit.AnimLibPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings({"deprecation", "removal"})
public class MaterialParser {
    
    private static final Method LEGACY_MATCH, GET_KEY;
    private static final Map<String, String> LEGACY_MATERIALS = new HashMap<>();
    
    static {
        Method legacy = null, getKey = null;
        try {
            legacy = Material.class.getDeclaredMethod("matchMaterial", String.class, boolean.class);
            getKey = Enchantment.class.getDeclaredMethod("getKey");
        } catch (Exception ex) {
            //
        }
        LEGACY_MATCH = legacy;
        GET_KEY = getKey;

        LEGACY_MATERIALS.put("130", "ENDER_CHEST");
        LEGACY_MATERIALS.put("278", "DIAMOND_PICKAXE");
        LEGACY_MATERIALS.put("279", "DIAMOND_AXE");
        LEGACY_MATERIALS.put("311", "DIAMOND_CHESTPLATE");
        LEGACY_MATERIALS.put("dye", "INK_SAC");
        LEGACY_MATERIALS.put("fish", "COD");
        LEGACY_MATERIALS.put("head", "PLAYER_HEAD");
        LEGACY_MATERIALS.put("player_skull", "PLAYER_HEAD");
        LEGACY_MATERIALS.put("skull", "PLAYER_HEAD");
        LEGACY_MATERIALS.put("skull_item", "PLAYER_HEAD");
        LEGACY_MATERIALS.put("spawn_egg", "PIG_SPAWN_EGG");
    }
    
    public static Material parse(String value) {
        String id = value.toLowerCase(Locale.ENGLISH).replace('-', '_');
        id = LEGACY_MATERIALS.getOrDefault(id, id);
        Material m = Material.matchMaterial(id);
        if (m == null && LEGACY_MATCH != null) {
            try {
                return (Material) LEGACY_MATCH.invoke(null, id, Boolean.TRUE);
            } catch (Exception ex) {
                return null;
            }
        }
        return m;
    }
    
    private static final Map<String, Enchantment> enchantments = new HashMap<>();
    private static final Method GET_ID;

    static {
        Map<String, Enchantment> map = enchantments;

        putEnchantment(map, "protection");
        putEnchantment(map, "fire_protection");
        putEnchantment(map, "feather_falling");
        putEnchantment(map, "blast_protection");
        putEnchantment(map, "projectile_protection");
        putEnchantment(map, "respiration");
        putEnchantment(map, "aqua_affinity");
        putEnchantment(map, "thorns");
        putEnchantment(map, "depth_strider");
        putEnchantment(map, "frost_walker");
        putEnchantment(map, "binding_curse");
        putEnchantment(map, "sharpness");
        putEnchantment(map, "smite");
        putEnchantment(map, "bane_of_arthropods");
        putEnchantment(map, "knockback");
        putEnchantment(map, "fire_aspect");
        putEnchantment(map, "looting");
        putEnchantment(map, "efficiency");
        putEnchantment(map, "silk_touch");
        putEnchantment(map, "unbreaking");
        putEnchantment(map, "fortune");
        putEnchantment(map, "power");
        putEnchantment(map, "punch");
        putEnchantment(map, "flame");
        putEnchantment(map, "infinity");
        putEnchantment(map, "luck_of_the_sea");
        putEnchantment(map, "lure");
        putEnchantment(map, "mending");
        putEnchantment(map, "vanishing_curse");

        Method getId = null;
        if (AnimLibPlugin.IS_LEGACY) {
            try {
                getId = Enchantment.class.getDeclaredMethod("getId");
            } catch (Exception ex) {
                // idk man
            }
        }
        GET_ID = getId;
        String name;
        for (Enchantment ench : Enchantment.values()) {
            if (ench != null) { // Weird, I know. Some person apparently had issues
                try {
                    name = GET_KEY == null ? ench.getName() : ((NamespacedKey) GET_KEY.invoke(ench)).getKey();
                } catch (Exception ex) {
                    continue;
                }
                if (name != null) {
                    if (getId != null) {
                        try {
                            map.put(getId.invoke(ench).toString(), ench);
                        } catch (Exception ex) {
                            // D:
                        }
                    }
                    map.putIfAbsent(name.toLowerCase(Locale.ENGLISH), ench);
                }
            }
        }
    }

    private static void putEnchantment(Map<String, Enchantment> map, String key) {
        Enchantment enchantment = Enchantment.getByKey(new NamespacedKey("minecraft", key));
        if (enchantment != null) {
            map.put(key, enchantment);
        }
    }

    public static int getId(Enchantment enchantment) {
        try {
            return (Integer) GET_ID.invoke(enchantment);
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public static Enchantment getEnchantment(String id) {
        return enchantments.get(id.toLowerCase(Locale.ENGLISH).replace("-", "_"));
    }
}
