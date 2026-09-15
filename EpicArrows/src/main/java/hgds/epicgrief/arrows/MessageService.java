package hgds.epicgrief.arrows;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MessageService {

    private final JavaPlugin plugin;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendConfigured(CommandSender sender, String path) {
        sendConfigured(sender, path, Map.of());
    }

    public void sendConfigured(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = configured(path, placeholders);
        if (!message.isBlank()) {
            sender.sendMessage(message);
        }
    }

    public void broadcastConfigured(String path, Map<String, String> placeholders) {
        String message = configured(path, placeholders);
        if (!message.isBlank()) {
            Bukkit.broadcastMessage(message);
        }
    }

    public void send(CommandSender sender, String message) {
        String colored = color(message);
        if (!colored.isBlank()) {
            sender.sendMessage(colored);
        }
    }

    public String configured(String path, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString(path, "");
        if (message == null || message.isBlank()) {
            return "";
        }

        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue());
        }

        return prefix() + color(message);
    }

    public List<String> colorList(List<String> values) {
        List<String> colored = new ArrayList<>();
        for (String value : values) {
            colored.add(color(value));
        }

        return colored;
    }

    public String color(String value) {
        if (value == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String prefix() {
        return color(plugin.getConfig().getString("messages.prefix", ""));
    }
}
