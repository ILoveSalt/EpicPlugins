package hgds.epicgrief;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WoodRepository {

    private final Logger logger;
    private final File dataFile;
    private FileConfiguration data;
    private final Map<UUID, WoodPlayerData> cache = new HashMap<>();

    WoodRepository(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFile = new File(dataFolder, "playerdata.yml");
    }

    void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Could not create playerdata.yml", exception);
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
        cache.clear();
    }

    void save() {
        if (data == null) {
            return;
        }

        for (Map.Entry<UUID, WoodPlayerData> entry : cache.entrySet()) {
            write(entry.getKey(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not save playerdata.yml", exception);
        }
    }

    WoodPlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::read);
    }

    private WoodPlayerData read(UUID uuid) {
        String path = WoodPlayerData.path(uuid);
        int backpack = data.getInt(path + ".backpack", 0);
        double salary = data.getDouble(path + ".salary", 0.0);
        long lastCutAt = data.getLong(path + ".last-cut-at", 0L);
        return new WoodPlayerData(backpack, salary, lastCutAt);
    }

    private void write(UUID uuid, WoodPlayerData playerData) {
        String path = WoodPlayerData.path(uuid);
        data.set(path + ".backpack", playerData.getBackpack());
        data.set(path + ".salary", playerData.getSalary());
        data.set(path + ".last-cut-at", playerData.getLastCutAt());
    }
}
