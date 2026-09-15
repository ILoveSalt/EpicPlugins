package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WoodScoreboardService {

    private static final long UPDATE_PERIOD_TICKS = 20L;

    private final EpicWoodCutter plugin;
    private final Map<UUID, Scoreboard> activeBoards = new HashMap<>();
    private final Set<UUID> playersInRegion = new HashSet<>();
    private BukkitTask updateTask;

    WoodScoreboardService(EpicWoodCutter plugin) {
        this.plugin = plugin;
    }

    void start() {
        stop();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, UPDATE_PERIOD_TICKS, UPDATE_PERIOD_TICKS);
    }

    void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (UUID uuid : new HashSet<>(activeBoards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hide(player);
            }
        }

        activeBoards.clear();
        playersInRegion.clear();
    }

    void handleRegionEnter(Player player) {
        playersInRegion.add(player.getUniqueId());
        show(player);
    }

    void handleRegionLeave(Player player) {
        playersInRegion.remove(player.getUniqueId());
        hide(player);
    }

    void refresh(Player player) {
        if (!playersInRegion.contains(player.getUniqueId())) {
            return;
        }

        show(player);
    }

    private void refreshAll() {
        for (UUID uuid : new HashSet<>(playersInRegion)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                playersInRegion.remove(uuid);
                activeBoards.remove(uuid);
                continue;
            }

            if (!plugin.regions().isInWoodRegion(player.getLocation())) {
                handleRegionLeave(player);
                continue;
            }

            show(player);
        }
    }

    private void show(Player player) {
        WoodPlayerData data = plugin.repository().get(player.getUniqueId());
        WoodGroupSettings group = plugin.config().groups().resolve(player);
        List<String> lines = plugin.config().scoreboardLines();

        Scoreboard scoreboard = activeBoards.computeIfAbsent(player.getUniqueId(), ignored -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective objective = scoreboard.getObjective("woodcutter");
        Component title = plugin.config().color(plugin.config().scoreboardTitle());
        if (objective == null) {
            objective = scoreboard.registerNewObjective("woodcutter", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(title);
        }

        clearEntries(scoreboard);

        int score = lines.size();
        for (String line : lines) {
            Component formatted = plugin.config().color(plugin.config().formatLine(player, data, group, line));
            String entry = uniqueEntry(score);
            Team team = scoreboard.registerNewTeam("line" + score);
            team.addEntry(entry);
            team.prefix(formatted);
            objective.getScore(entry).setScore(score);
            score--;
        }

        player.setScoreboard(scoreboard);
    }

    private void hide(Player player) {
        UUID uuid = player.getUniqueId();
        activeBoards.remove(uuid);

        if (player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void clearEntries(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective("woodcutter");
        if (objective != null) {
            for (String entry : new HashSet<>(scoreboard.getEntries())) {
                scoreboard.resetScores(entry);
            }
        }

        for (Team team : new HashSet<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("line")) {
                team.unregister();
            }
        }
    }

    private String uniqueEntry(int score) {
        return ChatColor.values()[score % ChatColor.values().length].toString() + ChatColor.RESET;
    }
}
