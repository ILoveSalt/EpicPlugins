package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpecCommand implements CommandExecutor, TabCompleter {
    private final StaffService service;

    public SpecCommand(StaffService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            service.sendConfigured(sender, "messages.player-only", Map.of());
            return true;
        }

        if (args.length == 0) {
            service.requestSpec(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            service.sendConfigured(sender, "messages.player-not-found", Map.of("player", args[0]));
            return true;
        }

        service.teleportToSpecTarget(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1 || !service.canReceiveSpec(player)) {
            return Collections.emptyList();
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                .sorted()
                .toList();
    }
}
