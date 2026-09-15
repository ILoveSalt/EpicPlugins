package hgds.epicgrief.api.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public interface ConfigAPI {
    FileConfiguration loadConfig(JavaPlugin paramJavaPlugin, String paramString);

    FileConfiguration getConfig(JavaPlugin paramJavaPlugin, String paramString);

    void saveConfig(JavaPlugin paramJavaPlugin, String paramString);

    void unloadConfig(JavaPlugin paramJavaPlugin, String paramString);

    void reloadConfig(JavaPlugin paramJavaPlugin, String paramString);
}
