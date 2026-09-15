package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

final class WoodCommand implements CommandExecutor, TabCompleter {

    private final EpicWoodCutter plugin;

    WoodCommand(EpicWoodCutter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.config().color(plugin.config().message("usage")));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("epicwoodcutter.admin")) {
                sender.sendMessage(plugin.config().color(plugin.config().message("noPermission")));
                return true;
            }

            plugin.reloadPlugin();
            sender.sendMessage(plugin.config().color(plugin.config().message("config-reloaded")));
            return true;
        }

        if (subCommand.equals("salary")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.config().color(plugin.config().message("console")));
                return true;
            }

            if (!player.hasPermission("epicwoodcutter.use")) {
                player.sendMessage(plugin.config().color(plugin.config().message("noPermission")));
                return true;
            }

            plugin.paySalary(player);
            return true;
        }

        sender.sendMessage(plugin.config().color(plugin.config().message("unknown")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("salary", "reload").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
