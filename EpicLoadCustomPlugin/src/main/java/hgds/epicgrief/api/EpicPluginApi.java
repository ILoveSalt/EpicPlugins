package hgds.epicgrief.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;

public interface EpicPluginApi {

    EpicPluginConnection connect(JavaPlugin plugin);

    EpicPluginConnection connect(JavaPlugin plugin, EpicPlugin epicPlugin);

    boolean disconnect(JavaPlugin plugin);

    Optional<EpicPluginConnection> getConnection(String pluginName);

    Collection<EpicPluginConnection> getConnections();
}
