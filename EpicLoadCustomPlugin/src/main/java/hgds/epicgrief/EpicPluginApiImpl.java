package hgds.epicgrief;

import hgds.epicgrief.api.EpicPlugin;
import hgds.epicgrief.api.EpicPluginApi;
import hgds.epicgrief.api.EpicPluginConnection;
import hgds.epicgrief.api.EpicPluginContext;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

final class EpicPluginApiImpl implements EpicPluginApi {

    private final JavaPlugin loaderPlugin;
    private final Map<String, EpicPluginConnection> connections = new LinkedHashMap<>();

    EpicPluginApiImpl(JavaPlugin loaderPlugin) {
        this.loaderPlugin = Objects.requireNonNull(loaderPlugin, "loaderPlugin");
    }

    @Override
    public synchronized EpicPluginConnection connect(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (!(plugin instanceof EpicPlugin epicPlugin)) {
            throw new IllegalArgumentException(plugin.getName() + " must implement EpicPlugin");
        }

        return connect(plugin, epicPlugin);
    }

    @Override
    public synchronized EpicPluginConnection connect(JavaPlugin plugin, EpicPlugin epicPlugin) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(epicPlugin, "epicPlugin");

        String key = normalize(plugin.getName());
        if (connections.containsKey(key)) {
            throw new IllegalStateException("Epic plugin already connected: " + plugin.getName());
        }

        EpicPluginContext context = new EpicPluginContext(loaderPlugin, plugin);
        epicPlugin.onEpicPluginConnected(context);

        EpicPluginConnection connection = new EpicPluginConnection(plugin, epicPlugin, Instant.now());
        connections.put(key, connection);
        loaderPlugin.getLogger().info("Epic-плагин подключен через API: " + plugin.getName());
        return connection;
    }

    @Override
    public synchronized boolean disconnect(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        EpicPluginConnection connection = connections.remove(normalize(plugin.getName()));
        if (connection == null) {
            return false;
        }

        EpicPluginContext context = new EpicPluginContext(loaderPlugin, connection.getBukkitPlugin());
        try {
            connection.getEpicPlugin().onEpicPluginDisconnected(context);
        } catch (RuntimeException exception) {
            loaderPlugin.getLogger().log(Level.SEVERE, "Ошибка при отключении Epic-плагина " + connection.getName(), exception);
        } finally {
            loaderPlugin.getLogger().info("Epic-плагин отключен от API: " + connection.getName());
        }

        return true;
    }

    @Override
    public synchronized Optional<EpicPluginConnection> getConnection(String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName");
        return Optional.ofNullable(connections.get(normalize(pluginName)));
    }

    @Override
    public synchronized Collection<EpicPluginConnection> getConnections() {
        return List.copyOf(connections.values());
    }

    synchronized void disconnectAll() {
        for (EpicPluginConnection connection : new ArrayList<>(connections.values())) {
            disconnect(connection.getBukkitPlugin());
        }
    }

    private String normalize(String pluginName) {
        return pluginName.toLowerCase(Locale.ROOT);
    }
}
