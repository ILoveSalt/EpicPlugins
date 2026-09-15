package hgds.epicgrief.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Загрузка и сохранение yaml-файлов плагина в кодировке UTF-8.
 * Отсутствующие ключи дополняются значениями из встроенного в jar ресурса.
 */
public final class Config {

    private static JavaPlugin plugin;

    private Config() {
    }

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Загружает файл из папки плагина. Если файла нет — создаёт из ресурса в jar
     * (или пустой, если ресурса тоже нет).
     */
    public static FileConfiguration getData(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration config = new YamlConfiguration();

        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                save(config, fileName); // создаём пустой файл
            }
        }

        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                config.load(reader);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getLogger().severe("Не удалось загрузить " + fileName + ": " + e.getMessage());
            }
        }

        // Значения по умолчанию из встроенного ресурса (для новых ключей)
        InputStream defaultsStream = plugin.getResource(fileName);
        if (defaultsStream != null) {
            try (Reader reader = new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = new YamlConfiguration();
                defaults.load(reader);
                config.setDefaults(defaults);
                config.options().copyDefaults(false);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getLogger().severe("Не удалось прочитать встроенный " + fileName + ": " + e.getMessage());
            }
        }
        return config;
    }

    public static void save(FileConfiguration config, String fileName) {
        try {
            config.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить " + fileName + ": " + e.getMessage());
        }
    }
}
