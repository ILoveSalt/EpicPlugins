package hgds.epicgrief.lib.commands;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.commands.CommandSource;
import hgds.epicgrief.api.commands.depend.CommandIssuer;
import hgds.epicgrief.api.commands.depend.CommandTabComplete;
import hgds.epicgrief.api.permission.PermissionsAPI;
import hgds.epicgrief.api.utils.Cooldown;
import hgds.epicgrief.market.messaging.Lang;

public class CraftCommand extends Command implements CommandSource {
    private static final PermissionsAPI PERMISSIONS_API = GalaxyAPI.getPermissionsAPI();

    private final CommandManager commandManager;

    private CommandIssuer commandIssuer;

    private CommandTabComplete commandTabComplete;

    private boolean onlyConsole;

    private boolean onlyPlayers;

    private int cooldown;

    private String cooldownType;

    CraftCommand(CommandManager commandManager, String command, CommandIssuer commandIssuer, String... aliases) {
        super(command, "", "", Arrays.asList(aliases));
        this.commandManager = commandManager;
        this.commandIssuer = commandIssuer;
        this.cooldown = 5;
        this.cooldownType = "command_cooldown";
        registerCommand();
        commandManager.add(this);
    }

    /**
     * Регистрация без рефлексии: Paper предоставляет публичный CommandMap.register(...)
     * и CommandMap.getKnownCommands(). Paper автоматически пересинхронизирует дерево
     * команд с клиентами при вызове register(...) на новых версиях.
     */
    private void registerCommand() {
        CommandMap commandMap = this.commandManager.getCommandMap();
        commandMap.register("epicauction", this);
        List<String> commands = new ArrayList<>(getAliases());
        commands.add(getName());
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        commands.forEach(command -> knownCommands.put(command.toLowerCase(), this));
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String alias, String[] args) throws IllegalArgumentException {
        if (this.onlyPlayers && !(commandSender instanceof Player))
            return ImmutableList.of();
        if (commandSender == null)
            return ImmutableList.of();
        if (this.commandTabComplete == null)
            return super.tabComplete(commandSender, alias, args);
        List<String> complete = this.commandTabComplete.getComplete(commandSender, alias, args);
        if (complete == null)
            return super.tabComplete(commandSender, alias, args);
        return complete.parallelStream().limit(15L).collect(Collectors.toList());
    }

    @Override
    public void setCommandIssuer(CommandIssuer commandIssuer) {
        this.commandIssuer = commandIssuer;
    }

    @Override
    public void setOnlyConsole(boolean flag) {
        this.onlyConsole = flag;
    }

    @Override
    public void setCooldown(String key, int seconds) {
        this.cooldown = seconds * 20;
        this.cooldownType = key;
    }

    @Override
    public Command getCommand() {
        return this;
    }

    @Override
    public void setCommandTabComplete(CommandTabComplete tabComplete) {
        this.commandTabComplete = tabComplete;
    }

    @Override
    public void setOnlyPlayers(boolean flag) {
        this.onlyPlayers = flag;
    }

    @Override
    public void disable() {
        GalaxyAPI.getCommandsAPI().disableCommand(this);
    }

    @Override
    public boolean execute(CommandSender commandSender, String commandName, String[] args) {
        boolean checkPlayer = commandSender instanceof Player;
        if (!checkPlayer && this.onlyPlayers) {
            Lang.sendMessage(commandSender, "COMMAND_ONLY_PLAYERS");
            return false;
        }
        if (checkPlayer && this.onlyConsole) {
            Lang.sendMessage(commandSender, "COMMAND_ONLY_CONSOLE");
            return false;
        }
        if (checkPlayer) {
            Player player = (Player) commandSender;
            if (!player.isOnline())
                return false;
            if (Cooldown.hasCooldown(player.getName(), this.cooldownType)) {
                if (this.cooldown != 5) {
                    int time = Cooldown.getCooldownLeft(player.getName(), this.cooldownType);
                    Lang.sendMessage(commandSender, "COOLDOWN", time);
                }
                return false;
            }
            Cooldown.addCooldown(player.getName(), this.cooldownType, this.cooldown);
        }
        fixArgs(args);
        this.commandIssuer.execute(commandSender, commandName, args);
        return true;
    }

    private void fixArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.length() > 2) {
                char c0 = arg.charAt(0);
                char cl = arg.charAt(arg.length() - 1);
                if ((c0 == '[' && cl == ']') || (c0 == '<' && cl == '>'))
                    args[i] = arg.substring(1, arg.length() - 1);
            }
        }
    }
}
