package hgds.epicgrief.lib.configuration;

import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.listener.GListener;
import hgds.epicgrief.market.Market;

public class ConfigListener extends GListener<Market> {
    private final ConfigManager configManager;

    protected ConfigListener(ConfigManager configManager) {
        super(Market.getInstance());
        this.configManager = configManager;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        JavaPlugin javaPlugin = (JavaPlugin) e.getPlugin();
        this.configManager.getConfigs().remove(javaPlugin);
    }
}
