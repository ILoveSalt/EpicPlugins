package hgds.epicgrief;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EpicChatCommand implements CommandExecutor, TabCompleter {
    private final EpicChat plugin;

    public EpicChatCommand(EpicChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("EpicChat " + plugin.getDescription().getVersion());
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("epicchat.reload") && !sender.hasPermission("chat.admin")) {
                plugin.sendConfiguredMessage(sender, "NO_PERMISSION");
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage("EpicChat reloaded.");
            return true;
        }

        sender.sendMessage("/" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || (!sender.hasPermission("epicchat.reload") && !sender.hasPermission("chat.admin"))) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if ("reload".startsWith(prefix)) {
            result.add("reload");
        }
        return result;
    }
}
