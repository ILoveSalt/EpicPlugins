package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StaffCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "epicstaffcontrol.admin";

    private final StaffService service;

    public StaffCommand(StaffService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "on" -> withPlayer(sender, service::startWork);
            case "off" -> withPlayer(sender, player -> service.stopWork(player, true));
            case "salary" -> withPlayer(sender, service::paySalary);
            case "up" -> withPlayer(sender, service::promote);
            case "list" -> service.sendRankList(sender);
            case "info" -> showInfo(sender, args);
            case "stats" -> showStats(sender, args);
            case "give" -> giveRank(sender, args);
            case "take" -> takeRank(sender, args);
            case "setpunishments" -> setPunishments(sender, args);
            case "reload" -> reload(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void showInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String requestedRank = joinTail(args, 1);
            RankDefinition rank = service.getRank(service.normalizeRankId(requestedRank));
            if (rank == null) {
                service.sendConfigured(sender, "messages.invalid-rank", Map.of("rank", requestedRank));
                return;
            }
            service.sendRankInfo(sender, rank);
            return;
        }

        if (sender instanceof Player player) {
            StaffProfile profile = service.getProfile(player);
            RankDefinition rank = service.getRank(profile.getRank());
            if (rank == null) {
                service.sendConfigured(sender, "messages.no-rank", Map.of("player", player.getName()));
                return;
            }
            service.sendRankInfo(sender, rank);
            return;
        }

        service.sendConfigured(sender, "messages.invalid-rank", Map.of("rank", "?"));
    }

    private void showStats(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!requireAdmin(sender)) {
                return;
            }
            OfflinePlayer target = findPlayer(sender, args[1]);
            if (target != null) {
                service.sendStats(sender, target);
            }
            return;
        }

        if (sender instanceof Player player) {
            service.sendStats(sender, player);
        } else {
            service.sendConfigured(sender, "messages.player-only", Map.of());
        }
    }

    private void giveRank(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            service.sendConfigured(sender, "messages.usage-give", Map.of());
            return;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target == null) {
            return;
        }

        RankDefinition rank = service.getRank(service.normalizeRankId(joinTail(args, 2)));
        if (rank == null) {
            service.sendConfigured(sender, "messages.invalid-rank", Map.of("rank", joinTail(args, 2)));
            return;
        }
        service.giveRank(sender, target, rank);
    }

    private void takeRank(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            service.sendConfigured(sender, "messages.usage-take", Map.of());
            return;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target != null) {
            service.takeRank(sender, target);
        }
    }

    private void setPunishments(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            service.sendConfigured(sender, "messages.usage-setpunishments", Map.of());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            service.sendConfigured(sender, "messages.invalid-number", Map.of("value", args[2]));
            return;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target != null) {
            service.setPunishments(sender, target, amount);
        }
    }

    private void reload(CommandSender sender) {
        if (requireAdmin(sender)) {
            service.reload(sender);
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        service.sendConfigured(sender, "messages.no-permission", Map.of());
        return false;
    }

    private OfflinePlayer findPlayer(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) {
            return offline;
        }

        service.sendConfigured(sender, "messages.player-not-found", Map.of("player", name));
        return null;
    }

    private void withPlayer(CommandSender sender, PlayerAction action) {
        if (sender instanceof Player player) {
            action.run(player);
        } else {
            service.sendConfigured(sender, "messages.player-only", Map.of());
        }
    }

    private void sendHelp(CommandSender sender) {
        service.sendConfigured(sender, "messages.help-header", Map.of());
        sender.sendMessage("§e/staff on§7 - начать смену");
        sender.sendMessage("§e/staff off§7 - закончить смену");
        sender.sendMessage("§e/staff salary§7 - получить зарплату");
        sender.sendMessage("§e/staff info [должность]§7 - информация");
        sender.sendMessage("§e/staff stats§7 - ваша статистика");
        sender.sendMessage("§e/staff up§7 - повысить должность");
        sender.sendMessage("§e/staff list§7 - список должностей");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§6/staff give <игрок> <должность>");
            sender.sendMessage("§6/staff take <игрок>");
            sender.sendMessage("§6/staff stats <игрок>");
            sender.sendMessage("§6/staff setpunishments <игрок> <число>");
            sender.sendMessage("§6/staff reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("on", "off", "salary", "info", "stats", "up", "list", "help"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                commands.addAll(List.of("give", "take", "setpunishments", "reload"));
            }
            return filter(commands, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("give", "take", "stats", "setpunishments").contains(subcommand)) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                return Collections.emptyList();
            }
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if ((subcommand.equals("info") && args.length == 2) || (subcommand.equals("give") && args.length == 3)) {
            return filter(service.getRanks().stream().map(RankDefinition::id).toList(), args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .sorted()
                .toList();
    }

    private String joinTail(String[] args, int start) {
        return String.join(" ", List.of(args).subList(start, args.length));
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run(Player player);
    }
}
