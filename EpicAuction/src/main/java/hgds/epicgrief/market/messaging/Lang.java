package hgds.epicgrief.market.messaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.configuration.ConfigAPI;
import hgds.epicgrief.market.Market;

public final class Lang {
    private Lang() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final ConfigAPI CONFIG_API = GalaxyAPI.getConfigAPI();

    public static Market javaPlugin;

    private static FileConfiguration messagesConfig;

    private static final Map<String, String> MESSAGES = new ConcurrentHashMap<>();

    public static void load(Market javaPlugin) {
        if (Lang.javaPlugin != null)
            throw new UnsupportedOperationException("Lang is loaded");
        Lang.javaPlugin = javaPlugin;
        messagesConfig = CONFIG_API.loadConfig(javaPlugin, "lang.yml");
        loadMessages();
    }

    private static void loadMessages() {
        MESSAGES.clear();
        messagesConfig.getConfigurationSection("").getValues(false).keySet().forEach(key -> {
            Object value = messagesConfig.get(key);
            MESSAGES.put(key, (value instanceof List)
                    ? String.join("\n", messagesConfig.getStringList(key))
                    : messagesConfig.getString(key));
        });
    }

    public static void reload() {
        CONFIG_API.reloadConfig(javaPlugin, "lang.yml");
        loadMessages();
    }

    private static String notFound(String key) {
        return "§cMessage not found: " + key;
    }

    public static void sendMessage(CommandSender sender, String key, Object... replace) {
        String message = MESSAGES.get(key);
        if (message == null) {
            sender.sendMessage(notFound(key));
            return;
        }
        message = format(key, message, replace);
        if (message.contains("\n")) {
            Arrays.stream(message.split("\n")).forEach(sender::sendMessage);
            return;
        }
        sender.sendMessage(message);
    }

    public static List<String> getList(String key, Object... replace) {
        String message = MESSAGES.get(key);
        if (message == null)
            return Collections.singletonList(notFound(key));
        return Arrays.asList(format(key, message, replace).split("\n"));
    }

    public static String getMessage(String key, Object... replace) {
        String message = MESSAGES.get(key);
        if (message == null)
            return notFound(key);
        return format(key, message, replace);
    }

    private static String format(String key, String message, Object... replace) {
        try {
            return String.format(message, replace);
        } catch (MissingFormatArgumentException ex) {
            StringBuilder parameters = new StringBuilder();
            for (Object param : replace) {
                if (parameters.length() > 0)
                    parameters.append("; ");
                if (param == null) {
                    parameters.append("null");
                } else {
                    parameters.append(param.getClass()).append(" (").append(param).append(")");
                }
            }
            throw new IllegalArgumentException("Format error for lang key " + key
                    + " (args: " + replace.length + "): " + parameters);
        }
    }
}
