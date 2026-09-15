package hgds.epicgrief.util;

import java.util.Map;
import org.bukkit.ChatColor;

public final class StringUtil {
    public static String colorize(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String format(String string, Map<String, String> args) {
        for (Map.Entry<String, String> arg : args.entrySet())
            string = string.replace("{" + (String)arg.getKey() + "}", arg.getValue());
        return string;
    }
}
