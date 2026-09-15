package hgds.epicgrief;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import hgds.epicgrief.utils.Utils;

/**
 * Админ-команда /epiccooldowns (алиас /ecooldowns):
 * reload, save, clear <игрок>, clearall. Право: epiccooldown.admin.
 */
public final class EpicCooldownCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "save", "clear", "clearall");

    private final CooldownPlugin plugin;

    public EpicCooldownCommand(CooldownPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Utils.has(sender, "epiccooldown.admin")) {
            return true;
        }
        if (args.length == 0) {
            Utils.sendMessage(sender, Utils.getMessage("usage"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                Utils.reloadConfig();
                this.plugin.getCommandResolver().rebuild();
                this.plugin.getCooldowns().startAutoSave();
                Utils.sendMessage(sender, Utils.getMessage("reloaded"));
            }
            case "save" -> {
                this.plugin.getCooldowns().save();
                Utils.sendMessage(sender, Utils.getMessage("saved")
                        .replace("%count%", String.valueOf(this.plugin.getCooldowns().size())));
            }
            case "clear" -> {
                if (args.length < 2) {
                    Utils.sendMessage(sender, Utils.getMessage("usage"));
                    return true;
                }
                this.plugin.getCooldowns().clearPlayer(args[1]);
                Utils.sendMessage(sender, Utils.getMessage("cleared").replace("%player%", args[1]));
            }
            case "clearall" -> {
                this.plugin.getCooldowns().clearAll();
                Utils.sendMessage(sender, Utils.getMessage("cleared-all"));
            }
            default -> Utils.sendMessage(sender, Utils.getMessage("usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("epiccooldown.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return partial(args[0], SUBCOMMANDS);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return partial(args[1], names);
        }
        return List.of();
    }

    private static List<String> partial(String token, List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}