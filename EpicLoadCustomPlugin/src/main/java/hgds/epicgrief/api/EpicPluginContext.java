package hgds.epicgrief.api;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public final class EpicPluginContext {

    private final JavaPlugin loaderPlugin;
    private final JavaPlugin connectedPlugin;

    public EpicPluginContext(JavaPlugin loaderPlugin, JavaPlugin connectedPlugin) {
        this.loaderPlugin = Objects.requireNonNull(loaderPlugin, "loaderPlugin");
        this.connectedPlugin = Objects.requireNonNull(connectedPlugin, "connectedPlugin");
    }

    public JavaPlugin getLoaderPlugin() {
        return loaderPlugin;
    }

    public JavaPlugin getConnectedPlugin() {
        return connectedPlugin;
    }

    public Server getServer() {
        return loaderPlugin.getServer();
    }

    public Logger getLogger() {
        return loaderPlugin.getLogger();
    }
}
