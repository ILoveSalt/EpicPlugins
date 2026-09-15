package hgds.epicgrief.lib.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final Map<JavaPlugin, ConcurrentHashMap<String, FileConfiguration>> configs = new ConcurrentHashMap<>();

    public void addConfig(JavaPlugin javaPlugin, String configName, FileConfiguration fileConfiguration) {
        this.configs.computeIfAbsent(javaPlugin, key -> new ConcurrentHashMap<>())
                .put(configName, fileConfiguration);
    }

    public void removeConfig(JavaPlugin javaPlugin, String configName) {
        ConcurrentHashMap<String, FileConfiguration> pluginConfigs = this.configs.get(javaPlugin);
        if (pluginConfigs == null)
            return;
        pluginConfigs.remove(configName);
        if (pluginConfigs.isEmpty())
            this.configs.remove(javaPlugin);
    }

    public FileConfiguration getConfig(JavaPlugin javaPlugin, String configName) {
        ConcurrentHashMap<String, FileConfiguration> pluginConfigs = this.configs.get(javaPlugin);
        return (pluginConfigs == null) ? null : pluginConfigs.get(configName);
    }

    public Map<JavaPlugin, ConcurrentHashMap<String, FileConfiguration>> getConfigs() {
        return this.configs;
    }
}
