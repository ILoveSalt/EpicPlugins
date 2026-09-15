package hgds.epicgrief.bosses;

import org.bukkit.ChatColor;
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

    public void sendConfiguredMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        List<String> lines = configuredLines(path, placeholders);
        for (String line : lines) {
            if (!line.isBlank()) {
                sender.sendMessage(line);
            }
        }
    }

    public void sendRaw(CommandSender sender, String message) {
        if (message != null && !message.isBlank()) {
            sender.sendMessage(color(message));
        }
    }

    public String color(String text) {
        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> configuredLines(String path, Map<String, String> placeholders) {
        List<String> rawLines;
        if (plugin.getConfig().isList(path)) {
            rawLines = plugin.getConfig().getStringList(path);
        } else {
            String message = plugin.getConfig().getString(path, "");
            rawLines = message == null ? List.of() : List.of(message.split("\\R", -1));
        }

        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine == null ? "" : rawLine;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace(entry.getKey(), entry.getValue());
            }
            lines.add(color(line));
        }
        return lines;
    }
}
