package hgds.epicgrief.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Objects;

public final class EpicPluginConnection {

    private final JavaPlugin bukkitPlugin;
    private final EpicPlugin epicPlugin;
    private final Instant connectedAt;

    public EpicPluginConnection(JavaPlugin bukkitPlugin, EpicPlugin epicPlugin, Instant connectedAt) {
        this.bukkitPlugin = Objects.requireNonNull(bukkitPlugin, "bukkitPlugin");
        this.epicPlugin = Objects.requireNonNull(epicPlugin, "epicPlugin");
        this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt");
    }

    public String getName() {
        return bukkitPlugin.getName();
    }

    public JavaPlugin getBukkitPlugin() {
        return bukkitPlugin;
    }

    public EpicPlugin getEpicPlugin() {
        return epicPlugin;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }
}
