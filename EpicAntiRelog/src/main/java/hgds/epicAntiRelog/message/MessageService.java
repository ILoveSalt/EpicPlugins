package hgds.epicAntiRelog.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageService {

    private final JavaPlugin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendConfiguredMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = configuredMessage(path, placeholders);
        if (!message.isBlank()) {
            sender.sendMessage(message);
        }
    }

    public void broadcastConfiguredMessage(String path, Map<String, String> placeholders) {
        String message = configuredMessage(path, placeholders);
        if (!message.isBlank()) {
            Bukkit.broadcastMessage(message);
        }
    }

    public void sendConfiguredTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        String title = configuredMessage(titlePath, placeholders);
        String subtitle = configuredMessage(subtitlePath, placeholders);

        if (!title.isBlank() || !subtitle.isBlank()) {
            player.sendTitle(title, subtitle, 10, 50, 10);
        }
    }

    public void sendConfiguredActionBar(Player player, String path, Map<String, String> placeholders) {
        String message = configuredMessage(path, placeholders);
        if (!message.isBlank()) {
            player.sendActionBar(toComponent(message));
        }
    }

    public String configuredMessage(String path, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString(path, "");
        if (message == null || message.isBlank()) {
            return "";
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return color(message);
    }

    public Map<String, String> playerPlaceholders(Player player, int time) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%time%", String.valueOf(time));
        placeholders.put("%formated-sec%", formatSeconds(time));
        return placeholders;
    }

    public List<String> colorList(List<String> values) {
        List<String> colored = new ArrayList<>();
        for (String value : values) {
            colored.add(color(value));
        }

        return colored;
    }

    public String color(String text) {
        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private Component toComponent(String legacyMessage) {
        return legacySerializer.deserialize(legacyMessage);
    }

    private String formatSeconds(int seconds) {
        int mod100 = seconds % 100;
        int mod10 = seconds % 10;

        if (mod100 >= 11 && mod100 <= 14) {
            return "\u0441\u0435\u043a\u0443\u043d\u0434";
        }

        if (mod10 == 1) {
            return "\u0441\u0435\u043a\u0443\u043d\u0434\u0430";
        }

        if (mod10 >= 2 && mod10 <= 4) {
            return "\u0441\u0435\u043a\u0443\u043d\u0434\u044b";
        }

        return "\u0441\u0435\u043a\u0443\u043d\u0434";
    }
}
