package hgds.epicAntiRelog.combat;

import hgds.epicAntiRelog.command.ConsoleCommandService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatService {

    public static final int TICKS_PER_SECOND = 20;

    private final JavaPlugin plugin;
    private final AntiRelogSettings settings;
    private final MessageService messages;
    private final ConsoleCommandService consoleCommands;
    private final Map<UUID, CombatTag> combatTags = new HashMap<>();

    private BukkitTask timerTask;

    public CombatService(
            JavaPlugin plugin,
            AntiRelogSettings settings,
            MessageService messages,
            ConsoleCommandService consoleCommands
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.consoleCommands = consoleCommands;
    }

    public void start() {
        timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickCombatTags, TICKS_PER_SECOND, TICKS_PER_SECOND);
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        for (CombatTag tag : new ArrayList<>(combatTags.values())) {
            removeBossBar(tag);
        }

        combatTags.clear();
    }

    public void tagPlayer(Player player, boolean disablePowerups) {
        long now = System.currentTimeMillis();
        long endsAt = now + (settings.pvpTimeSeconds() * 1000L);
        UUID playerId = player.getUniqueId();
        CombatTag tag = combatTags.get(playerId);
        boolean started = tag == null || tag.endsAt() <= now;

        if (tag == null) {
            tag = new CombatTag(playerId);
            combatTags.put(playerId, tag);
        }

        tag.playerName(player.getName());
        tag.endsAt(endsAt);
        updateBossBar(player, tag);

        if (started) {
            sendCombatStartedMessages(player);
        }

        if (disablePowerups && settings.disablePowerups()) {
            disablePowerups(player);
        }
    }

    public void clearCombat(Player player, boolean notify) {
        CombatTag tag = combatTags.remove(player.getUniqueId());
        if (tag == null) {
            return;
        }

        removeBossBar(tag);

        if (notify && player.isOnline()) {
            sendCombatStoppedMessages(player);
        }
    }

    public boolean isInCombat(Player player) {
        CombatTag tag = combatTags.get(player.getUniqueId());
        return tag != null && tag.endsAt() > System.currentTimeMillis();
    }

    public Map<String, String> placeholders(Player player) {
        return messages.playerPlaceholders(player, remainingSeconds(player));
    }

    public int remainingSeconds(Player player) {
        CombatTag tag = combatTags.get(player.getUniqueId());
        if (tag == null) {
            return 0;
        }

        long remainingMillis = Math.max(0L, tag.endsAt() - System.currentTimeMillis());
        return (int) Math.ceil(remainingMillis / 1000.0D);
    }

    private void tickCombatTags() {
        long now = System.currentTimeMillis();

        for (CombatTag tag : new ArrayList<>(combatTags.values())) {
            Player player = Bukkit.getPlayer(tag.playerId());
            if (player == null || !player.isOnline()) {
                clearCombat(tag.playerId());
                continue;
            }

            if (tag.endsAt() <= now) {
                clearCombat(player, true);
                continue;
            }

            updateBossBar(player, tag);
            messages.sendConfiguredActionBar(player, "messages.in-pvp-actionbar", placeholders(player));
        }
    }

    private void clearCombat(UUID playerId) {
        CombatTag tag = combatTags.remove(playerId);
        if (tag != null) {
            removeBossBar(tag);
        }
    }

    private void disablePowerups(Player player) {
        boolean disabled = false;

        if (player.isFlying()) {
            player.setFlying(false);
            disabled = true;
        }

        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            disabled = true;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            disabled = true;
        }

        if (!disabled) {
            return;
        }

        messages.sendConfiguredMessage(player, "messages.pvp-started-with-powerups", placeholders(player));
        consoleCommands.runCommands(settings.stringList("commands-on-powerups-disable"), placeholders(player));
    }

    private void updateBossBar(Player player, CombatTag tag) {
        String message = messages.configuredMessage("messages.in-pvp-bossbar", placeholders(player));

        if (message.isBlank()) {
            removeBossBar(tag);
            return;
        }

        if (tag.bossBar() == null) {
            tag.bossBar(Bukkit.createBossBar(message, BarColor.YELLOW, BarStyle.SOLID));
            tag.bossBar().addPlayer(player);
        }

        tag.bossBar().setTitle(message);
        tag.bossBar().setProgress(calculateProgress(tag));

        if (!tag.bossBar().getPlayers().contains(player)) {
            tag.bossBar().addPlayer(player);
        }
    }

    private double calculateProgress(CombatTag tag) {
        long remaining = Math.max(0L, tag.endsAt() - System.currentTimeMillis());
        double fullTime = Math.max(1.0D, settings.pvpTimeSeconds() * 1000.0D);
        return Math.max(0.0D, Math.min(1.0D, remaining / fullTime));
    }

    private void removeBossBar(CombatTag tag) {
        BossBar bossBar = tag.bossBar();
        if (bossBar != null) {
            bossBar.removeAll();
            tag.bossBar(null);
        }
    }

    private void sendCombatStartedMessages(Player player) {
        Map<String, String> placeholders = placeholders(player);
        messages.sendConfiguredMessage(player, "messages.pvp-started", placeholders);
        messages.sendConfiguredTitle(player, "messages.pvp-started-title", "messages.pvp-started-subtitle", placeholders);
    }

    private void sendCombatStoppedMessages(Player player) {
        Map<String, String> placeholders = placeholders(player);
        messages.sendConfiguredMessage(player, "messages.pvp-stopped", placeholders);
        messages.sendConfiguredTitle(player, "messages.pvp-stopped-title", "messages.pvp-stopped-subtitle", placeholders);
        messages.sendConfiguredActionBar(player, "messages.pvp-stopped-actionbar", placeholders);
    }
}
