package hgds.epicAntiRelog.punishment;

import hgds.epicAntiRelog.combat.CombatService;
import hgds.epicAntiRelog.command.ConsoleCommandService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PunishmentService {

    private final AntiRelogSettings settings;
    private final MessageService messages;
    private final CombatService combatService;
    private final ConsoleCommandService consoleCommands;

    public PunishmentService(
            AntiRelogSettings settings,
            MessageService messages,
            CombatService combatService,
            ConsoleCommandService consoleCommands
    ) {
        this.settings = settings;
        this.messages = messages;
        this.combatService = combatService;
        this.consoleCommands = consoleCommands;
    }

    public void punishCombatLeave(Player player, boolean kicked) {
        Map<String, String> placeholders = combatService.placeholders(player);
        messages.broadcastConfiguredMessage("messages.pvp-leaved", placeholders);

        if (kicked ? settings.killOnKick() : settings.killOnLeave()) {
            killPlayer(player);
        }

        if (!kicked || settings.runCommandsOnKick()) {
            consoleCommands.runCommands(settings.stringList("commands-on-leave"), placeholders);
        }
    }

    public boolean matchesKickReason(String reason) {
        List<String> configuredReasons = settings.stringList("kick-messages");
        if (configuredReasons.isEmpty()) {
            return true;
        }

        String normalizedReason = normalizeReason(reason);
        for (String configuredReason : configuredReasons) {
            String normalizedConfigured = normalizeReason(configuredReason);
            if (!normalizedConfigured.isBlank() && normalizedReason.contains(normalizedConfigured)) {
                return true;
            }
        }

        return false;
    }

    private void killPlayer(Player player) {
        if (!player.isDead() && player.getHealth() > 0.0D) {
            player.setHealth(0.0D);
        }
    }

    private String normalizeReason(String reason) {
        return ChatColor.stripColor(messages.color(reason)).toLowerCase(Locale.ROOT);
    }
}
