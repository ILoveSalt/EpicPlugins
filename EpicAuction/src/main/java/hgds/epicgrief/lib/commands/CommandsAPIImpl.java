package hgds.epicgrief.lib.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import hgds.epicgrief.api.commands.CommandSource;
import hgds.epicgrief.api.commands.CommandsAPI;
import hgds.epicgrief.api.commands.depend.CommandIssuer;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandsAPIImpl implements CommandsAPI {
    private final CommandManager commandManager = new CommandManager();

    @Override
    public CommandSource register(String commandName, CommandIssuer commandIssuer, String[] aliases) {
        return new CraftCommand(this.commandManager, commandName, commandIssuer, aliases);
    }

    @Override
    public CommandSource getCommand(String commandName) {
        return this.commandManager.getCommand(commandName);
    }

    @Override
    public Map<String, CommandSource> getCommands() {
        return this.commandManager.getCommands();
    }

    @Override
    public List<String> getCompleteString(Collection<String> seeList, String[] args) {
        String lastWord = args[args.length - 1];
        List<String> matched = new ArrayList<>();
        for (String string : seeList) {
            if (!string.toLowerCase().startsWith(lastWord.toLowerCase()))
                continue;
            matched.add(string);
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        return matched;
    }

    @Override
    public void disableCommand(String commandName) {
        Map<String, Command> knownCommands = this.commandManager.getCommandMap().getKnownCommands();
        Command command = knownCommands.remove(commandName.toLowerCase());
        if (command != null)
            command.getAliases().forEach(alias -> knownCommands.remove(alias.toLowerCase()));
        CommandSource commandSource = this.commandManager.getCommand(commandName);
        if (commandSource == null)
            return;
        this.commandManager.remove(commandSource);
    }

    @Override
    public void disableCommand(CommandSource commandSource) {
        this.commandManager.remove(commandSource);
    }

    @Override
    public void disableAllCommands(JavaPlugin javaPlugin) {
        if (javaPlugin == null)
            return;
        new ArrayList<>(this.commandManager.getCommands().keySet()).forEach(this::disableCommand);
    }
}
