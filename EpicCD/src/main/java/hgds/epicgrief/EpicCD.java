package hgds.epicgrief;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class EpicCD extends JavaPlugin implements Listener {
    private static final List<String> MODE_ORDER = List.of("vanish", "god", "fly");

    private final Set<String> blockedCommands = new HashSet<>();
    private final Map<UUID, Map<String, Long>> messageCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        String mode = getRestrictedMode(player, "pickup-items");
        if (mode == null) {
            return;
        }

        event.setCancelled(true);
        sendConfiguredMessage(
                player,
                "message-" + mode + "-pickup",
                "pickup:" + mode,
                getConfig().getLong("cooldown-pickup", 0L)
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String mode = getRestrictedMode(player, "block-place");
        if (mode == null) {
            return;
        }

        event.setCancelled(true);
        sendConfiguredMessage(player, "message-" + mode + "-place", "place:" + mode, 0L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (blockedCommands.isEmpty()) {
            return;
        }

        String command = normalizeCommand(event.getMessage());
        if (!isBlockedCommand(command)) {
            return;
        }

        Player player = event.getPlayer();
        String mode = getActiveMode(player);
        if (mode == null) {
            return;
        }

        event.setCancelled(true);
        sendConfiguredMessage(
                player,
                "message-" + mode + "-cmdblock",
                "command:" + mode,
                getCommandCooldown(mode)
        );
    }

    private void reloadSettings() {
        blockedCommands.clear();

        for (String command : getConfig().getStringList("non_working_command")) {
            String normalized = normalizeCommand(command);
            if (!normalized.isEmpty()) {
                blockedCommands.add(normalized);
            }
        }
    }

    private String getRestrictedMode(Player player, String configPath) {
        for (String mode : MODE_ORDER) {
            if (getConfig().getBoolean(mode + "." + configPath, false) && isModeActive(player, mode)) {
                return mode;
            }
        }

        return null;
    }

    private String getActiveMode(Player player) {
        for (String mode : MODE_ORDER) {
            if (isModeActive(player, mode)) {
                return mode;
            }
        }

        return null;
    }

    private boolean isModeActive(Player player, String mode) {
        return switch (mode) {
            case "vanish" -> isVanishActive(player);
            case "god" -> isGodActive(player);
            case "fly" -> isFlyActive(player);
            default -> false;
        };
    }

    private boolean isVanishActive(Player player) {
        if (hasTruthyMetadata(player, "vanished", "vanish", "essentials:vanished", "essentials_vanish",
                "cmi_vanished", "supervanish", "premiumvanish")) {
            return true;
        }

        if (hasScoreboardTag(player, "vanish", "vanished")) {
            return true;
        }

        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            if (!onlinePlayer.equals(player) && !onlinePlayer.canSee(player)) {
                return true;
            }
        }

        return false;
    }

    private boolean isGodActive(Player player) {
        if (hasTruthyMetadata(player, "god", "godmode", "godMode", "essentials:god", "essentials_god",
                "cmi_god")) {
            return true;
        }

        return !isCreativeOrSpectator(player) && player.isInvulnerable();
    }

    private boolean isFlyActive(Player player) {
        if (hasTruthyMetadata(player, "fly", "flight", "flying", "essentials:fly", "essentials_fly",
                "cmi_fly")) {
            return true;
        }

        if (hasScoreboardTag(player, "fly", "flight", "flying")) {
            return true;
        }

        return !isCreativeOrSpectator(player) && player.getAllowFlight();
    }

    private boolean hasTruthyMetadata(Player player, String... keys) {
        for (String key : keys) {
            if (!player.hasMetadata(key)) {
                continue;
            }

            for (MetadataValue value : player.getMetadata(key)) {
                Object rawValue = value.value();
                if (rawValue == null) {
                    continue;
                }

                if (rawValue instanceof Boolean booleanValue) {
                    if (booleanValue) {
                        return true;
                    }
                    continue;
                }

                if (rawValue instanceof Number numberValue) {
                    if (numberValue.intValue() != 0) {
                        return true;
                    }
                    continue;
                }

                String textValue = rawValue.toString().trim();
                if (!textValue.isEmpty()
                        && !"false".equalsIgnoreCase(textValue)
                        && !"0".equals(textValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasScoreboardTag(Player player, String... tags) {
        Set<String> playerTags = new HashSet<>();
        for (String tag : player.getScoreboardTags()) {
            playerTags.add(tag.toLowerCase(Locale.ROOT));
        }

        for (String tag : tags) {
            if (playerTags.contains(tag.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private boolean isCreativeOrSpectator(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private boolean isBlockedCommand(String command) {
        for (String blockedCommand : blockedCommands) {
            if (command.equals(blockedCommand) || command.startsWith(blockedCommand + " ")) {
                return true;
            }
        }

        return false;
    }

    private String normalizeCommand(String command) {
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized.replaceAll("\\s+", " ");
    }

    private long getCommandCooldown(String mode) {
        long fallback = getConfig().getLong("\u0441ooldown-command." + mode, 0L);
        return getConfig().getLong("cooldown-command." + mode, fallback);
    }

    private void sendConfiguredMessage(Player player, String configPath, String cooldownKey, long cooldownSeconds) {
        if (!canSendMessage(player.getUniqueId(), cooldownKey, cooldownSeconds)) {
            return;
        }

        String message = getConfig().getString(configPath, "");
        if (message == null || message.isBlank()) {
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private boolean canSendMessage(UUID playerId, String key, long cooldownSeconds) {
        if (cooldownSeconds <= 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = messageCooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>());
        long availableAt = playerCooldowns.getOrDefault(key, 0L);
        if (availableAt > now) {
            return false;
        }

        playerCooldowns.put(key, now + TimeUnit.SECONDS.toMillis(cooldownSeconds));
        return true;
    }
}
