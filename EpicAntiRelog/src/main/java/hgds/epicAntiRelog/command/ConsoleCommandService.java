package hgds.epicAntiRelog.command;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ConsoleCommandService {

    private final JavaPlugin plugin;

    public ConsoleCommandService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runCommands(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }

            String preparedCommand = command;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                preparedCommand = preparedCommand.replace(entry.getKey(), entry.getValue());
            }

            while (preparedCommand.startsWith("/")) {
                preparedCommand = preparedCommand.substring(1);
            }

            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), preparedCommand);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not execute console command: " + preparedCommand, exception);
            }
        }
    }
}
