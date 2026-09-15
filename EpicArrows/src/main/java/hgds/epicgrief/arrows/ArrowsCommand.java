package hgds.epicgrief.arrows;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArrowsCommand implements TabExecutor {

    private static final String GIVE_PERMISSION = "epicarrows.command.give";
    private static final String RELOAD_PERMISSION = "epicarrows.command.reload";
    private static final String MANAGE_PERMISSION = "epicarrows.command.manage";
    private static final int MAX_AMOUNT = 2304;

    private final JavaPlugin plugin;
    private final ArrowConfigService configService;
    private final ArrowItemService itemService;
    private final MessageService messages;

    public ArrowsCommand(
            JavaPlugin plugin,
            ArrowConfigService configService,
            ArrowItemService itemService,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.itemService = itemService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> giveArrow(sender, args);
            case "disable" -> disableArrow(sender, args);
            case "enable" -> enableArrow(sender, args);
            case "list" -> listArrows(sender);
            case "reload" -> reload(sender);
            default -> {
                messages.sendConfigured(sender, "messages.unknown-command");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (canGive(sender)) {
                commands.add("give");
            }
            if (canReload(sender)) {
                commands.add("reload");
            }
            if (canManage(sender)) {
                commands.add("disable");
                commands.add("enable");
                commands.add("list");
            }
            return partialMatches(args[0], commands);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && canGive(sender)) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return partialMatches(args[1], players);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && canGive(sender)) {
            return partialMatches(args[2], configService.arrowKeys());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give") && canGive(sender)) {
            return partialMatches(args[3], Arrays.asList("1", "16", "32", "64"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("disable") && canManage(sender)) {
            List<String> enabledArrows = new ArrayList<>();
            for (String arrowKey : configService.arrowKeys()) {
                if (!configService.isDisabledArrow(arrowKey)) {
                    enabledArrows.add(arrowKey);
                }
            }
            return partialMatches(args[1], enabledArrows);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("enable") && canManage(sender)) {
            return partialMatches(args[1], configService.disabledArrowKeys());
        }

        return Collections.emptyList();
    }

    private boolean giveArrow(CommandSender sender, String[] args) {
        if (!canGive(sender)) {
            messages.sendConfigured(sender, "messages.no-permission");
            return true;
        }

        if (args.length < 3 || args.length > 4) {
            messages.sendConfigured(sender, "messages.give.usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.sendConfigured(sender, "messages.give.player-null");
            return true;
        }

        ArrowDefinition arrow = configService.arrow(args[2]);
        if (arrow == null) {
            messages.sendConfigured(sender, "messages.give.item-null");
            return true;
        }

        int amount = args.length == 4 ? parseAmount(sender, args[3]) : arrow.amount();
        if (amount < 1) {
            return true;
        }

        itemService.giveItem(target, arrow, amount);
        messages.sendConfigured(sender, "messages.give.gived");
        messages.sendConfigured(target, "messages.give.received", Map.of(
                "%name%", messages.color(arrow.title()),
                "%amount%", String.valueOf(amount)
        ));
        return true;
    }

    private boolean disableArrow(CommandSender sender, String[] args) {
        if (!canManage(sender)) {
            messages.sendConfigured(sender, "messages.no-permission");
            return true;
        }

        if (args.length != 2) {
            messages.send(sender, "&e&lARROW &8- &fUsage: &e/arrows disable <arrow>");
            return true;
        }

        ArrowDefinition arrow = configService.arrow(args[1]);
        if (arrow == null) {
            messages.sendConfigured(sender, "messages.give.item-null");
            return true;
        }

        boolean wasDisabled = configService.isDisabledArrow(arrow);
        configService.disableArrow(arrow.key());
        if (wasDisabled) {
            messages.send(sender, "&e&lARROW &8- &fArrow &e" + arrow.key() + " &fis already disabled.");
            return true;
        }

        messages.send(sender, "&e&lARROW &8- &fArrow &e" + arrow.key() + " &fdisabled.");
        messages.broadcastConfigured("messages.disable.broadcast", Map.of("%name%", messages.color(arrow.title())));
        return true;
    }

    private boolean enableArrow(CommandSender sender, String[] args) {
        if (!canManage(sender)) {
            messages.sendConfigured(sender, "messages.no-permission");
            return true;
        }

        if (args.length != 2) {
            messages.send(sender, "&e&lARROW &8- &fUsage: &e/arrows enable <arrow>");
            return true;
        }

        ArrowDefinition arrow = configService.arrow(args[1]);
        if (arrow == null) {
            messages.sendConfigured(sender, "messages.give.item-null");
            return true;
        }

        boolean wasDisabled = configService.isDisabledArrow(arrow);
        configService.enableArrow(arrow.key());
        if (!wasDisabled) {
            messages.send(sender, "&e&lARROW &8- &fArrow &e" + arrow.key() + " &fis already enabled.");
            return true;
        }

        messages.send(sender, "&e&lARROW &8- &fArrow &e" + arrow.key() + " &fenabled.");
        return true;
    }

    private boolean listArrows(CommandSender sender) {
        if (!canManage(sender)) {
            messages.sendConfigured(sender, "messages.no-permission");
            return true;
        }

        messages.send(sender, "&e&lARROW &8- &fArrows:");
        for (String arrowKey : configService.arrowKeys()) {
            String state = configService.isDisabledArrow(arrowKey) ? "&cdisabled" : "&aenabled";
            messages.send(sender, "&8- &e" + arrowKey + " &7(" + state + "&7)");
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!canReload(sender)) {
            messages.sendConfigured(sender, "messages.no-permission");
            return true;
        }

        plugin.reloadConfig();
        configService.reload();
        messages.sendConfigured(sender, "messages.reload.reload");
        return true;
    }

    private int parseAmount(CommandSender sender, String rawAmount) {
        int amount;
        try {
            amount = Integer.parseInt(rawAmount);
        } catch (NumberFormatException exception) {
            messages.send(sender, "&cAmount must be a number from 1 to " + MAX_AMOUNT + ".");
            return -1;
        }

        if (amount < 1 || amount > MAX_AMOUNT) {
            messages.send(sender, "&cAmount must be a number from 1 to " + MAX_AMOUNT + ".");
            return -1;
        }

        return amount;
    }

    private void sendUsage(CommandSender sender) {
        if (canGive(sender)) {
            messages.sendConfigured(sender, "messages.give.usage");
        }
        if (canManage(sender)) {
            messages.send(sender, "&e/arrows disable <arrow> &8- &fdisable custom arrow");
            messages.send(sender, "&e/arrows enable <arrow> &8- &fenable custom arrow");
            messages.send(sender, "&e/arrows list &8- &fshow arrows state");
        }
        if (canReload(sender)) {
            messages.sendConfigured(sender, "messages.reload.usage");
        }
    }

    private boolean canGive(CommandSender sender) {
        return sender.hasPermission(GIVE_PERMISSION) || sender.hasPermission("epicarrows.admin");
    }

    private boolean canReload(CommandSender sender) {
        return sender.hasPermission(RELOAD_PERMISSION) || sender.hasPermission("epicarrows.admin");
    }

    private boolean canManage(CommandSender sender) {
        return sender.hasPermission(MANAGE_PERMISSION) || sender.hasPermission("epicarrows.admin");
    }

    private List<String> partialMatches(String token, List<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, matches);
        Collections.sort(matches);
        return matches;
    }
}
