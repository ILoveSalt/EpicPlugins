package hgds.epicgrief.api.builders.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import hgds.epicgrief.api.utils.Head;

/**
 * Современный билдер предметов для новых версий Paper (MC 26.x / Java 25).
 * Убраны устаревшие API: legacy-ID материалов, short-damage конструкторы,
 * PotionData, SpawnEggMeta и т.п.
 */
public class ItemBuilder {
    private final ItemStack itemStack;

    public static ItemBuilder newBuilder(String id) {
        return new ItemBuilder(id);
    }

    public static ItemBuilder newBuilder(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder newBuilder(ItemStack item) {
        return new ItemBuilder(item);
    }

    private ItemBuilder(String input) {
        String[] args = input.split(":");
        if (args.length < 1 || args.length > 2)
            throw new IllegalArgumentException("Invalid item id: " + input);
        String name = args[0];
        String meta = (args.length > 1) ? args[1] : null;
        Material material;
        if (name.equalsIgnoreCase("HEAD")) {
            if (meta == null)
                throw new IllegalArgumentException("Invalid item id: " + input);
            if (meta.length() <= 16) {
                this.itemStack = Head.getHeadByName(meta);
            } else {
                this.itemStack = Head.getHeadByValue(meta);
            }
            return;
        }
        name = name.toUpperCase(Locale.ROOT).replace(" ", "_");
        material = Material.matchMaterial(name);
        if (material == null)
            throw new IllegalArgumentException("Unknown material: " + input);
        if (material == Material.AIR) {
            if (meta != null)
                throw new IllegalArgumentException("Invalid item id: " + input);
            this.itemStack = new ItemStack(Material.AIR);
            return;
        }
        short damage = 0;
        if (meta != null) {
            try {
                damage = Short.parseShort(meta);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid item id: " + input);
            }
        }
        this.itemStack = new ItemStack(material, 1);
        if (damage > 0)
            applyDamage(damage);
    }

    private void applyDamage(short damage) {
        ItemMeta im = this.itemStack.getItemMeta();
        if (im instanceof Damageable) {
            ((Damageable) im).setDamage(damage);
            this.itemStack.setItemMeta(im);
        }
    }

    private ItemBuilder(ItemStack is) {
        this.itemStack = is;
    }

    private ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material, 1);
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(Math.min(amount, 64));
        return this;
    }

    public ItemBuilder removeFlags() {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        Arrays.stream(ItemFlag.values()).forEach(itemMeta::addItemFlags);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder removeEnchantment() {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.getEnchants().keySet().forEach(itemMeta::removeEnchant);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setUnbreakable() {
        return setUnbreakable(true);
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.setUnbreakable(unbreakable);
        this.itemStack.setItemMeta(itemMeta);
        addFlag(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    public ItemBuilder glowing() {
        return glowing(true);
    }


    public ItemBuilder glowing(boolean glow) {
        if (!glow) {
            new ArrayList<>(this.itemStack.getEnchantments().keySet())
                    .forEach(this.itemStack::removeEnchantment);
            return this;
        }
        addFlag(ItemFlag.HIDE_ENCHANTS);
        this.itemStack.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment type, int level) {
        this.itemStack.addUnsafeEnchantment(type, level);
        return this;
    }

    public ItemBuilder setDurability(int durability) {
        applyDamage((short) durability);
        return this;
    }

    public ItemBuilder setName(String name) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.setDisplayName(name);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.setLore(new ArrayList<>(lore));
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        return addLore(Arrays.asList(lore));
    }

    public ItemBuilder addLore(List<String> lore) {
        ItemMeta im = this.itemStack.getItemMeta();
        List<String> oldLore = im.getLore();
        if (oldLore != null) {
            oldLore.addAll(lore);
        } else {
            oldLore = new ArrayList<>(lore);
        }
        im.setLore(oldLore);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder replaceLore(String in, String out) {
        ItemMeta im = this.itemStack.getItemMeta();
        List<String> lore = im.getLore();
        if (lore != null) {
            im.setLore(lore.stream()
                    .map(line -> line.replace(in, out))
                    .collect(Collectors.toList()));
            this.itemStack.setItemMeta(im);
        }
        return this;
    }

    public ItemBuilder addFlag(ItemFlag... flags) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.addItemFlags(flags);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder addFlag(ItemFlag flag) {
        return addFlag(new ItemFlag[] { flag });
    }

    public ItemBuilder setColor(Color color) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) itemMeta).setColor(color);
            this.itemStack.setItemMeta(itemMeta);
        } else if (itemMeta instanceof PotionMeta) {
            ((PotionMeta) itemMeta).setColor(color);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    public ItemBuilder addCustomEffect(PotionEffect effect, boolean overwrite) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta instanceof PotionMeta) {
            ((PotionMeta) itemMeta).addCustomEffect(effect, overwrite);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    public ItemBuilder setPotionData(PotionType type) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        if (itemMeta instanceof PotionMeta) {
            ((PotionMeta) itemMeta).setBasePotionType(type);
            this.itemStack.setItemMeta(itemMeta);
        }
        return this;
    }

    public ItemStack create() {
        return this.itemStack;
    }
}
