package hgds.epicgrief.api.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public abstract class GListener<JavaPlugin> implements Listener {
    protected final JavaPlugin javaPlugin;

    protected GListener(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        Bukkit.getPluginManager().registerEvents(this, (Plugin)javaPlugin);
    }

    public void unregisterListener() {
        HandlerList.unregisterAll(this);
    }
}
