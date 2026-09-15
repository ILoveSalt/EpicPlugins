package hgds.epicgrief.tools;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDurability(short durability) {
        this.item.setDurability(durability);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        ItemMeta meta = this.item.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder enchantall(int level) {
        byte b;
        int i;
        Enchantment[] arrayOfEnchantment;
        for (i = (arrayOfEnchantment = Enchantment.values()).length, b = 0; b < i; ) {
            Enchantment enchantment = arrayOfEnchantment[b];
            enchant(enchantment, level);
            b++;
        }
        return this;
    }

    public ItemBuilder flag(ItemFlag flag) {
        ItemMeta meta = this.item.getItemMeta();
        meta.addItemFlags(new ItemFlag[] { flag });
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder flagall() {
        byte b;
        int i;
        ItemFlag[] arrayOfItemFlag;
        for (i = (arrayOfItemFlag = ItemFlag.values()).length, b = 0; b < i; ) {
            ItemFlag flag = arrayOfItemFlag[b];
            flag(flag);
            b++;
        }
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        ItemMeta meta = this.item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = this.item.getItemMeta();
        meta.setLore(Utils.color(lore));
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addLore(String line) {
        List<String> list;
        ItemMeta meta = this.item.getItemMeta();
        if (meta.hasLore()) {
            list = meta.getLore();
        } else {
            list = new ArrayList<>();
        }
        list.add(Utils.color(line));
        meta.setLore(list);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeLore(int page) {
        ItemMeta meta = this.item.getItemMeta();
        if (!meta.hasLore())
            return this;
        List<String> list = meta.getLore();
        if (page > list.size())
            return this;
        list.remove(page);
        meta.setLore(Utils.color(list));
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return this.item;
    }
}