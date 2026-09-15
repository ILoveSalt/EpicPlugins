package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnimationChatCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "epicanimationchat.admin";

    private final EpicAnimationChat plugin;
    private final TabAnimationService animationService;

    public AnimationChatCommand(EpicAnimationChat plugin, TabAnimationService animationService) {
        this.plugin = plugin;
        this.animationService = animationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            case "preview" -> preview(sender, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage(Component.text(
                    "Reloaded " + animationService.size() + " animations.",
                    NamedTextColor.GREEN
            ));
        } catch (Exception exception) {
            sender.sendMessage(Component.text(
                    "Reload failed: " + exception.getMessage(),
                    NamedTextColor.RED
            ));
            plugin.getLogger().severe("Reload failed: " + exception.getMessage());
        }
    }

    private void list(CommandSender sender) {
        if (animationService.size() == 0) {
            sender.sendMessage(Component.text(
                    "No animations loaded from " + animationService.getAnimationsFile(),
                    NamedTextColor.YELLOW
            ));
            return;
        }

        sender.sendMessage(Component.text(
                "TAB animations (" + animationService.size() + "): "
                        + String.join(", ", animationService.names()),
                NamedTextColor.GOLD
        ));
    }

    private void preview(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /eac preview <animation>", NamedTextColor.RED));
            return;
        }

        String name = args[1];
        if (!animationService.contains(name)) {
            sender.sendMessage(Component.text("Animation not found: " + name, NamedTextColor.RED));
            return;
        }

        Player source = sender instanceof Player player ? player : null;
        sender.sendMessage(
                Component.text("%animation:" + name + "% -> ", NamedTextColor.GRAY)
                        .append(animationService.getCurrentFrame(name, source))
        );
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " preview <animation>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("reload", "list", "preview"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return filter(animationService.names(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(value);
            }
        }
        return result;
    }
}
