package hgds.epicAntiRelog.command;

import hgds.epicAntiRelog.combat.CombatService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public final class CombatCommandService {

    private final AntiRelogSettings settings;
    private final MessageService messages;
    private final CombatService combatService;

    public CombatCommandService(
            AntiRelogSettings settings,
            MessageService messages,
            CombatService combatService
    ) {
        this.settings = settings;
        this.messages = messages;
        this.combatService = combatService;
    }

    public void handleCommand(PlayerCommandPreprocessEvent event) {
        ParsedCommand command = ParsedCommand.from(event.getMessage());
        Player player = event.getPlayer();

        if (cancelCommandAgainstCombatPlayer(event, command)) {
            return;
        }

        if (!combatService.isInCombat(player) || !settings.disableCommandsInPvp()) {
            return;
        }

        if (player.hasPermission("epicantirelog.bypass.commands") || isCommandAllowedInCombat(player, command)) {
            return;
        }

        event.setCancelled(true);
        messages.sendConfiguredMessage(player, "messages.commands-disabled", combatService.placeholders(player));
    }

    private boolean cancelCommandAgainstCombatPlayer(PlayerCommandPreprocessEvent event, ParsedCommand command) {
        if (command.arguments().isEmpty()) {
            return false;
        }

        if (command.hasArgument("-s") && command.label().contains("ban")) {
            messages.sendConfiguredMessage(event.getPlayer(), "messages.ban-error", combatService.placeholders(event.getPlayer()));
            event.setCancelled(true);
            return true;
        }

        if (!containsCommand(settings.stringList("not_pvp_command"), command.label())) {
            return false;
        }

        Player target = Bukkit.getPlayerExact(command.arguments().get(0));
        if (target == null || !combatService.isInCombat(target)) {
            return false;
        }

        event.setCancelled(true);
        String message = messages.color(settings.string("noKickMessage", ""));
        if (!message.isBlank()) {
            event.getPlayer().sendMessage(message);
        }
        return true;
    }

    private boolean isCommandAllowedInCombat(Player player, ParsedCommand command) {
        if (containsCommand(settings.stringList("commands-whitelist"), command.label())) {
            return true;
        }

        return hasBypassListPermission(player) && containsCommand(settings.stringList("commands-whitelist-bypass"), command.label());
    }

    private boolean hasBypassListPermission(Player player) {
        return player.hasPermission("epicantirelog.commands.bypass") || player.hasPermission("epicantirelog.bypass.commands");
    }

    private boolean containsCommand(List<String> commands, String label) {
        for (String command : commands) {
            if (normalizeCommand(command).equals(label)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }

        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int spaceIndex = normalized.indexOf(' ');
        if (spaceIndex >= 0) {
            normalized = normalized.substring(0, spaceIndex);
        }

        return normalized;
    }
}
