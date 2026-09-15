package hgds.epicgrief;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class SocialSpyCommand implements CommandExecutor, TabCompleter {
    private final EpicChat plugin;

    public SocialSpyCommand(EpicChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("chat.spy")) {
            plugin.sendConfiguredMessage(player, "NO_PERMISSION");
            return true;
        }

        boolean enabled = plugin.toggleSocialSpy(player);
        plugin.sendConfiguredMessage(player, enabled ? "SPY_ON" : "SPY_OFF");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
