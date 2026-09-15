package hgds.epicAntiRelog.command;

import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.item.ItemService;
import hgds.epicAntiRelog.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigItemCommand implements TabExecutor {

    private static final String GIVE_PERMISSION = "epicantirelog.command.give";
    private static final String RELOAD_PERMISSION = "epicantirelog.command.reload";
    private static final int MAX_AMOUNT = 2304;

    private final JavaPlugin plugin;
    private final AntiRelogSettings settings;
    private final MessageService messages;
    private final ItemService itemService;

    public ConfigItemCommand(
            JavaPlugin plugin,
            AntiRelogSettings settings,
            MessageService messages,
            ItemService itemService
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.itemService = itemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "give" -> giveConfiguredItem(sender, label, args);
            case "reload" -> reloadConfig(sender);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission(GIVE_PERMISSION)) {
                subCommands.add("give");
            }
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                subCommands.add("reload");
            }
            return partialMatches(args[0], subCommands);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission(GIVE_PERMISSION)) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return partialMatches(args[1], players);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission(GIVE_PERMISSION)) {
            return partialMatches(args[2], Arrays.asList("1", "16", "64"));
        }

        return Collections.emptyList();
    }

    private boolean giveConfiguredItem(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(GIVE_PERMISSION)) {
            send(sender, "&cYou do not have permission.");
            return true;
        }

        GiveRequest request = parseGiveRequest(sender, label, args);
        if (request == null) {
            return true;
        }

        ItemStack sample = itemService.createConfiguredItem(1);
        if (sample == null || sample.getType().isAir()) {
            send(sender, "&cItem is not configured correctly. Check item.material in config.yml.");
            return true;
        }

        int remaining = request.amount();
        int maxStackSize = Math.max(1, sample.getMaxStackSize());
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            ItemStack stack = itemService.createConfiguredItem(stackAmount);
            if (stack == null) {
                send(sender, "&cItem is not configured correctly. Check item.material in config.yml.");
                return true;
            }

            Map<Integer, ItemStack> leftovers = request.target().getInventory().addItem(stack);
            dropLeftovers(request.target(), leftovers);
            remaining -= stackAmount;
        }

        request.target().updateInventory();
        send(sender, "&e&lANTIRELOG &8- &fGave configured item to &e" + request.target().getName() + " &fx" + request.amount() + ".");
        if (!sender.equals(request.target())) {
            send(request.target(), "&e&lANTIRELOG &8- &fYou received configured item &ex" + request.amount() + "&f.");
        }

        return true;
    }

    private GiveRequest parseGiveRequest(CommandSender sender, String label, String[] args) {
        if (args.length > 3) {
            sendUsage(sender, label);
            return null;
        }

        Player target = null;
        int amount = 1;

        if (args.length == 1) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                send(sender, "&cUsage: /" + label + " give <player> [amount]");
                return null;
            }
        }

        if (args.length == 2) {
            if (sender instanceof Player player && isInteger(args[1])) {
                target = player;
                amount = parseAmount(sender, args[1]);
            } else {
                target = Bukkit.getPlayerExact(args[1]);
            }
        }

        if (args.length == 3) {
            target = Bukkit.getPlayerExact(args[1]);
            amount = parseAmount(sender, args[2]);
        }

        if (target == null) {
            send(sender, "&cPlayer is not online.");
            return null;
        }

        if (amount < 1) {
            return null;
        }

        return new GiveRequest(target, amount);
    }

    private int parseAmount(CommandSender sender, String rawAmount) {
        int amount;
        try {
            amount = Integer.parseInt(rawAmount);
        } catch (NumberFormatException exception) {
            send(sender, "&cAmount must be a number from 1 to " + MAX_AMOUNT + ".");
            return -1;
        }

        if (amount < 1 || amount > MAX_AMOUNT) {
            send(sender, "&cAmount must be a number from 1 to " + MAX_AMOUNT + ".");
            return -1;
        }

        return amount;
    }

    private boolean reloadConfig(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, "&cYou do not have permission.");
            return true;
        }

        plugin.reloadConfig();
        settings.reload();
        send(sender, "&e&lANTIRELOG &8- &fConfig reloaded.");
        return true;
    }

    private void dropLeftovers(Player player, Map<Integer, ItemStack> leftovers) {
        for (ItemStack leftover : leftovers.values()) {
            if (leftover != null && !leftover.getType().isAir()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        send(sender, "&e&lANTIRELOG &8- &fCommands:");
        if (sender.hasPermission(GIVE_PERMISSION)) {
            send(sender, "&e/" + label + " give [player] [amount] &8- &fgive item from config.yml");
        }
        if (sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, "&e/" + label + " reload &8- &freload config.yml");
        }
    }

    private List<String> partialMatches(String token, List<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, matches);
        Collections.sort(matches);
        return matches;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(messages.color(message));
    }

    private record GiveRequest(Player target, int amount) {
    }
}
