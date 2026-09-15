package hgds.epicgrief;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ConfiguredChannelExecutor implements CommandExecutor, TabCompleter {
    private final EpicChat plugin;
    private final String channelName;

    public ConfiguredChannelExecutor(EpicChat plugin, String channelName) {
        this.plugin = plugin;
        this.channelName = channelName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/" + label + " <message>");
            return true;
        }
        plugin.handleConfiguredChannelCommand((Player) sender, channelName, String.join(" ", args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
