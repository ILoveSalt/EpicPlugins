package hgds.epicAntiRelog.config;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AntiRelogSettings {

    private final JavaPlugin plugin;
    private Set<String> disabledWorlds = Set.of();

    public AntiRelogSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        List<String> worlds = plugin.getConfig().getStringList("disabledWorlds");
        Set<String> normalizedWorlds = new HashSet<>();

        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalizedWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        disabledWorlds = Set.copyOf(normalizedWorlds);
    }

    public int pvpTimeSeconds() {
        return Math.max(1, plugin.getConfig().getInt("pvp-time", 30));
    }

    public boolean isDisabledWorld(Player player) {
        return disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    public boolean disableCommandsInPvp() {
        return plugin.getConfig().getBoolean("disable-commands-in-pvp", true);
    }

    public boolean cancelInteractWithEntities() {
        return plugin.getConfig().getBoolean("cancel-interact-with-entities", false);
    }

    public boolean disableTeleportsInPvp() {
        return plugin.getConfig().getBoolean("disable-teleports-in-pvp", false);
    }

    public boolean hideJoinMessage() {
        return plugin.getConfig().getBoolean("hide-join-message", true);
    }

    public boolean hideLeaveMessage() {
        return plugin.getConfig().getBoolean("hide-leave-message", true);
    }

    public boolean hideDeathMessage() {
        return plugin.getConfig().getBoolean("hide-death-message", true);
    }

    public boolean killOnLeave() {
        return plugin.getConfig().getBoolean("kill-on-leave", true);
    }

    public boolean killOnKick() {
        return plugin.getConfig().getBoolean("kill-on-kick", true);
    }

    public boolean runCommandsOnKick() {
        return plugin.getConfig().getBoolean("run-commands-on-kick", true);
    }

    public boolean disablePowerups() {
        return plugin.getConfig().getBoolean("disable-powerups", true);
    }

    public int intValue(String path, int defaultValue) {
        return plugin.getConfig().getInt(path, defaultValue);
    }

    public String string(String path) {
        return plugin.getConfig().getString(path, "");
    }

    public String string(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }

    public List<String> stringList(String path) {
        return plugin.getConfig().getStringList(path);
    }
}
