package hgds.epicgrief.lib.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import hgds.epicgrief.api.commands.CommandSource;

/**
 * На новых версиях Paper CommandMap доступен напрямую через Bukkit.getCommandMap(),
 * рефлексия по CraftServer.commandMap больше не нужна.
 */
public class CommandManager {
    private final Map<String, CommandSource> commands = new ConcurrentHashMap<>();

    private final CommandMap commandMap = Bukkit.getCommandMap();

    public CommandSource getCommand(String commandName) {
        return this.commands.get(commandName);
    }

    public Map<String, CommandSource> getCommands() {
        return this.commands;
    }

    public void add(CommandSource commandSource) {
        this.commands.putIfAbsent(commandSource.getName(), commandSource);
    }

    public void remove(CommandSource commandSource) {
        this.commands.remove(commandSource.getName());
    }

    public CommandMap getCommandMap() {
        return this.commandMap;
    }
}
