package hgds.epicgrief.command;

import com.google.common.collect.ImmutableMap;
import hgds.epicgrief.CheckConfig;
import hgds.epicgrief.CheckManager;
import hgds.epicgrief.Main;
import hgds.epicgrief.ModeratorActType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CheckCommand implements CommandExecutor, TabCompleter {
    private static final List<String> MODERATOR_ACTIONS = Arrays.asList("confirm", "cancel", "leave", "deleted", "ignore");

    private final CheckConfig.Messages messages;

    private final CheckManager checkManager;

    public CheckCommand(Main plugin) {
        this.messages = plugin.getPluginConfig().getMessages();
        this.checkManager = plugin.getCheckManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("check.check")) {
            this.messages.getNotPermitted().send(player, ImmutableMap.of("permission", "check.check"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        Player actPlayer = Bukkit.getPlayerExact(args[0]);
        if (actPlayer == null) {
            this.messages.getPlayerNotFound().send(player, ImmutableMap.of("player", args[0]));
            return true;
        }
        if (args.length == 1) {
            checkPlayer(player, actPlayer);
        } else {
            actToCheckedPlayer(player, actPlayer, args[1]);
        }
        return true;
    }

    private void checkPlayer(Player sender, Player playerForCheck) {
        Player moderator = this.checkManager.getCheckModerator(playerForCheck);
        if (moderator == null) {
            if (sender == playerForCheck) {
                this.messages.getYouCannotCheckYourself().send(sender);
                return;
            }
            if (playerForCheck.hasPermission("check.moderator")) {
                this.messages.getCannotCheckModerator().send(sender, ImmutableMap.of("moderator", playerForCheck.getName()));
                return;
            }
            this.checkManager.checkPlayer(playerForCheck, sender);
            this.messages.getCheckingPlayer().send(sender, ImmutableMap.of("player", playerForCheck.getName()));
        } else if (moderator == sender) {
            this.messages.getYouAlreadyCheckingThisPlayer().send(sender, ImmutableMap.of("player", playerForCheck.getName()));
        } else {
            this.messages.getPlayerAlreadyBeingChecked().send(sender,
                    ImmutableMap.of("player", playerForCheck.getName(), "moderator", moderator.getName()));
        }
    }

    private void actToCheckedPlayer(Player sender, Player checkedPlayer, String actionString) {
        Player moderator = this.checkManager.getCheckModerator(checkedPlayer);
        if (moderator == null) {
            this.messages.getPlayerIsNotBeingChecked().send(sender, ImmutableMap.of("player", checkedPlayer.getName()));
            return;
        }
        if (moderator != sender) {
            this.messages.getPlayerAlreadyBeingChecked().send(sender,
                    ImmutableMap.of("player", checkedPlayer.getName(), "moderator", moderator.getName()));
            return;
        }
        ModeratorActType action;
        if (actionString.equalsIgnoreCase("confirm")) {
            action = ModeratorActType.CANCEL_CHECK;
        } else if (actionString.equalsIgnoreCase("cancel")) {
            action = ModeratorActType.CONFIRM_CHEATS;
        } else if (actionString.equalsIgnoreCase("leave")) {
            action = ModeratorActType.DODGED;
        } else if (actionString.equalsIgnoreCase("deleted")) {
            action = ModeratorActType.CHEAT_PROGRAM_FILES_FOUND;
        } else if (actionString.equalsIgnoreCase("ignore")) {
            action = ModeratorActType.IGNORED;
        } else {
            this.messages.getNoSuchModeratorAction().send(sender, ImmutableMap.of("action", actionString));
            return;
        }
        this.checkManager.actToCheckedPlayer(checkedPlayer, action);
    }

    private void sendHelp(Player sender) {
        this.messages.getCheckCommandHelp().send(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("check.check")) {
            return null;
        }
        if (args.length == 2) {
            Player checkedPlayer = Bukkit.getPlayerExact(args[0]);
            if (checkedPlayer != null && this.checkManager.isBeingChecked(checkedPlayer)) {
                List<String> result = new ArrayList<>();
                for (String action : MODERATOR_ACTIONS) {
                    if (action.startsWith(args[1])) {
                        result.add(action);
                    }
                }
                return result;
            }
            return Collections.emptyList();
        }
        if (args.length > 2) {
            return Collections.emptyList();
        }
        return null;
    }
}