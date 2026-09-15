package hgds.epicgrief;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

final class PotionCommand implements CommandExecutor, TabCompleter {
    private final EpicEditorPotion plugin;
    private final PotionManager potionManager;

    PotionCommand(EpicEditorPotion plugin, PotionManager potionManager) {
        this.plugin = plugin;
        this.potionManager = potionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "usage");
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            default -> {
                send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("epiceditorpotion.give")) {
            send(sender, "no_perm");
            return true;
        }
        if (args.length < 3 || args.length > 4) {
            send(sender, "usage");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            send(sender, "player_not_found", "%player%", args[1]);
            return true;
        }

        String potionId = args[2].toLowerCase(Locale.ROOT);
        if (!potionManager.contains(potionId)) {
            send(sender, "potion_not_found", "%potion%", args[2]);
            return true;
        }

        int amount = 1;
        if (args.length == 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                send(sender, "invalid_amount");
                return true;
            }
        }

        ItemStack item = potionManager.createItem(potionId);
        item.setAmount(amount);
        HashMap<Integer, ItemStack> leftovers = target.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        send(sender, "given",
                "%player%", target.getName(),
                "%potion%", potionId,
                "%amount%", String.valueOf(amount)
        );
        if (!sender.equals(target)) {
            send(target, "received",
                    "%potion%", potionId,
                    "%amount%", String.valueOf(amount)
            );
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!sender.hasPermission("epiceditorpotion.list")) {
            send(sender, "no_perm");
            return true;
        }

        send(sender, "list", "%potions%", String.join(", ", potionManager.ids()));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("epiceditorpotion.reload")) {
            send(sender, "no_perm");
            return true;
        }

        plugin.reloadConfig();
        potionManager.reload();
        send(sender, "reloaded", "%amount%", String.valueOf(potionManager.size()));
        return true;
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String prefix = plugin.getConfig().getString("message.prefix", "&6&lЗЕЛЬЯ");
        String message = plugin.getConfig().getString("message." + key, key);
        message = message.replace("%prefix%", prefix);

        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        sender.sendMessage(PotionManager.colorize(message));
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("epiceditorpotion.give")) {
                suggestions.add("give");
            }
            if (sender.hasPermission("epiceditorpotion.list")) {
                suggestions.add("list");
            }
            if (sender.hasPermission("epiceditorpotion.reload")) {
                suggestions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            suggestions.addAll(plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            suggestions.addAll(potionManager.ids());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            suggestions.addAll(List.of("1", "8", "16", "32", "64"));
        }

        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(input))
                .sorted()
                .toList();
    }
}
