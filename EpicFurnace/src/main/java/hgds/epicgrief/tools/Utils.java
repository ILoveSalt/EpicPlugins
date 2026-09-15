package hgds.epicgrief.tools;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class Utils {
    private static FileConfiguration config;

    public static FileConfiguration getConfig() {
        return (config != null) ? config : (config = Config.getFile("config.yml"));
    }

    public static String getMessage(String path) {
        return getConfig().getString("messages." + path);
    }

    public static String getString(String path) {
        return getConfig().getString(path);
    }

    public static List<String> getStringList(String path) {
        return getConfig().getStringList(path);
    }

    public static int getInt(String path) {
        return getConfig().getInt(path);
    }

    public static double getDouble(String path) {
        return getConfig().getDouble(path);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> text) {
        List<String> list = new ArrayList<>();
        text.forEach(x -> {

        });
        return list;
    }

    public static boolean has(CommandSender player, String permission) {
        if (!player.hasPermission(permission)) {
            sendMessage(player, getMessage("no-permission"));
            return false;
        }
        return true;
    }

    public static void sendMessage(CommandSender player, String text) {
        byte b;
        int i;
        String[] arrayOfString;
        for (i = (arrayOfString = text.split(";")).length, b = 0; b < i; ) {
            String line = arrayOfString[b];
            line = line.replace("%player%", player.getName());
            if (line.startsWith("title:")) {
                if (player instanceof Player)
                    Title.sendTitle((Player)player, line.split("title:")[1]);
            } else if (line.startsWith("actionbar:")) {
                if (player instanceof Player)
                    ActionBar.sendActionBar((Player)player, line.split("actionbar:")[1]);
            } else {
                player.sendMessage(color(String.valueOf(getMessage("prefix")) + line));
            }
            b++;
        }
    }

    public static ItemStack loadItem(FileConfiguration config, String path) {
        Material material = Material.getMaterial(config.getString(String.valueOf(path) + ".material").toUpperCase());
        ItemBuilder builder = new ItemBuilder(material);
        String name = config.getString(String.valueOf(path) + ".name");
        List<String> lore = config.getStringList(String.valueOf(path) + ".lore");
        if (name != null)
            builder.setDisplayName(name);
        if (lore != null)
            builder.setLore(lore);
        if (config.getString(String.valueOf(path) + ".enchants") != null)
            if (config.isString(String.valueOf(path) + ".enchants")) {
                String[] enchant = config.getString(String.valueOf(path) + ".enchants").split(";");
                if (enchant[0].equalsIgnoreCase("all")) {
                    builder.enchantall(Integer.valueOf(enchant[1]).intValue());
                } else {
                    builder.enchant(Enchantment.getByName(enchant[0].toUpperCase()), Integer.valueOf(enchant[1]).intValue());
                }
            } else if (config.isList(String.valueOf(path) + ".enchants")) {
                config.getStringList(String.valueOf(path) + ".enchants").forEach(x -> paramItemBuilder.enchant(Enchantment.getByName(x.split(";")[0].toUpperCase()), Integer.valueOf(x.split(";")[1]).intValue()));
            }
        if (config.getString(String.valueOf(path) + ".flags") != null)
            if (config.isString(String.valueOf(path) + ".flags")) {
                String flag = config.getString(String.valueOf(path) + ".flags");
                if (flag.equalsIgnoreCase("all")) {
                    builder.flagall();
                } else {
                    builder.flag(ItemFlag.valueOf(flag.toUpperCase()));
                }
            } else if (config.isList(String.valueOf(path) + ".flags")) {
                config.getStringList(String.valueOf(path) + ".flags").forEach(x -> paramItemBuilder.flag(ItemFlag.valueOf(x.toUpperCase())));
            }
        return builder.build();
    }

    public static List<ItemStack> loadItems(FileConfiguration config, String path) {
        List<ItemStack> items = new ArrayList<>();
        for (String item : config.getConfigurationSection(path).getKeys(false))
            items.add(loadItem(config, item));
        return items;
    }
}
