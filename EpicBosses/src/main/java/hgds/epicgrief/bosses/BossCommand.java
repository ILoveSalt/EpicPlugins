package hgds.epicgrief.bosses;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BossCommand implements TabExecutor {

    private static final String ADMIN_PERMISSION = "epicbosses.admin";
    private static final String RELOAD_PERMISSION = "epicbosses.command.reload";
    private static final String SPAWN_PERMISSION = "epicbosses.command.spawn";
    private static final String INFO_PERMISSION = "epicbosses.command.info";
    private static final String LOOT_PERMISSION = "epicbosses.command.loot";
    private static final String KILL_PERMISSION = "epicbosses.command.kill";

    private final BossConfigService configService;
    private final BossService bossService;
    private final MessageService messages;

    public BossCommand(BossConfigService configService, BossService bossService, MessageService messages) {
        this.configService = configService;
        this.bossService = bossService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "reload" -> reload(sender);
            case "spawn" -> spawn(sender, args);
            case "info" -> info(sender);
            case "loot" -> loot(sender, args);
            case "kill", "despawn" -> kill(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (has(sender, RELOAD_PERMISSION)) {
                subCommands.add("reload");
            }
            if (has(sender, SPAWN_PERMISSION)) {
                subCommands.add("spawn");
            }
            if (has(sender, INFO_PERMISSION)) {
                subCommands.add("info");
            }
            if (has(sender, LOOT_PERMISSION)) {
                subCommands.add("loot");
            }
            if (has(sender, KILL_PERMISSION)) {
                subCommands.add("kill");
            }
            return partialMatches(args[0], subCommands);
        }

        if (args.length == 2 && Arrays.asList("spawn", "loot").contains(args[0].toLowerCase(Locale.ROOT))) {
            return partialMatches(args[1], new ArrayList<>(configService.bosses().keySet()));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("loot")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return partialMatches(args[2], players);
        }

        return Collections.emptyList();
    }

    private boolean reload(CommandSender sender) {
        if (!has(sender, RELOAD_PERMISSION)) {
            sendNoPermission(sender);
            return true;
        }

        bossService.reload();
        messages.sendConfiguredMessage(sender, "messages.command.reload", Map.of());
        return true;
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!has(sender, SPAWN_PERMISSION)) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        BossService.SpawnResult result = bossService.spawn(args[1]);
        switch (result) {
            case NOT_FOUND -> messages.sendConfiguredMessage(sender, "messages.command.notExist", Map.of());
            case FAILED, ALREADY_ACTIVE -> messages.sendConfiguredMessage(sender, "messages.command.failedSpawn", Map.of());
            case SPAWNED -> messages.sendConfiguredMessage(sender, "messages.command.spawn", Map.of());
        }

        return true;
    }

    private boolean info(CommandSender sender) {
        if (!has(sender, INFO_PERMISSION)) {
            sendNoPermission(sender);
            return true;
        }

        BossService.BossSnapshot snapshot = bossService.snapshot();
        if (snapshot == null) {
            messages.sendConfiguredMessage(sender, "messages.command.info.notSpawned", Map.of());
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{id}", snapshot.id());
        placeholders.put("{name}", snapshot.name());
        placeholders.put("{type}", snapshot.type());
        placeholders.put("{health}", snapshot.health());
        placeholders.put("{maxHealth}", snapshot.maxHealth());
        messages.sendConfiguredMessage(sender, "messages.command.info.spawned", placeholders);
        return true;
    }

    private boolean loot(CommandSender sender, String[] args) {
        if (!has(sender, LOOT_PERMISSION)) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        BossDefinition definition = configService.boss(args[1]);
        if (definition == null) {
            messages.sendConfiguredMessage(sender, "messages.command.notExist", Map.of());
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                messages.sendRaw(sender, "&cPlayer is not online.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            messages.sendConfiguredMessage(sender, "messages.command.only_players", Map.of());
            return true;
        }

        bossService.giveRewards(definition, target);
        messages.sendConfiguredMessage(sender, "messages.command.loot", Map.of());
        return true;
    }

    private boolean kill(CommandSender sender) {
        if (!has(sender, KILL_PERMISSION)) {
            sendNoPermission(sender);
            return true;
        }

        if (bossService.despawnActive()) {
            messages.sendRaw(sender, "&6&lBOSSES: &fCurrent boss removed.");
        } else {
            messages.sendConfiguredMessage(sender, "messages.command.info.notSpawned", Map.of());
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        messages.sendConfiguredMessage(sender, "messages.command.help", Map.of());
    }

    private void sendNoPermission(CommandSender sender) {
        messages.sendConfiguredMessage(sender, "messages.no_permission", Map.of());
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(ADMIN_PERMISSION) || sender.hasPermission(permission);
    }

    private List<String> partialMatches(String token, List<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, matches);
        Collections.sort(matches);
        return matches;
    }
}
