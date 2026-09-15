package hgds.epicgrief.lib.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.configuration.ConfigAPI;

public class ConfigAPIImpl implements ConfigAPI {
    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    public ConfigAPIImpl() {
        new ConfigListener(CONFIG_MANAGER);
    }

    @Override
    public FileConfiguration loadConfig(JavaPlugin javaPlugin, String configName) {
        javaPlugin.saveResource(configName, false);
        File file = new File(javaPlugin.getDataFolder(), configName);
        try {
            if (!file.exists())
                file.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        try {
            yamlConfiguration.save(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        CONFIG_MANAGER.addConfig(javaPlugin, configName, yamlConfiguration);
        return yamlConfiguration;
    }

    @Override
    public FileConfiguration getConfig(JavaPlugin javaPlugin, String configName) {
        return CONFIG_MANAGER.getConfig(javaPlugin, configName);
    }

    @Override
    public void saveConfig(JavaPlugin javaPlugin, String configName) {
        File file = new File(javaPlugin.getDataFolder(), configName);
        try {
            if (!file.exists())
                file.createNewFile();
            FileConfiguration configuration = CONFIG_MANAGER.getConfig(javaPlugin, configName);
            if (configuration != null)
                configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unloadConfig(JavaPlugin javaPlugin, String configName) {
        CONFIG_MANAGER.removeConfig(javaPlugin, configName);
    }

    @Override
    public void reloadConfig(JavaPlugin javaPlugin, String configName) {
        File file = new File(javaPlugin.getDataFolder(), configName);
        try {
            if (!file.exists())
                file.createNewFile();
            FileConfiguration configuration = CONFIG_MANAGER.getConfig(javaPlugin, configName);
            if (configuration != null)
                configuration.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
