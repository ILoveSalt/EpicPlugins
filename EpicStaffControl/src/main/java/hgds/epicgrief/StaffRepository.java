package hgds.epicgrief;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StaffRepository {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Map<UUID, StaffProfile> profiles = new LinkedHashMap<>();

    public StaffRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public StaffProfile getOrCreate(UUID uniqueId, String name) {
        StaffProfile profile = profiles.computeIfAbsent(uniqueId, id -> new StaffProfile(id, name));
        if (name != null && !name.isBlank()) {
            profile.setLastName(name);
        }
        return profile;
    }

    public StaffProfile get(UUID uniqueId) {
        return profiles.get(uniqueId);
    }

    public void remove(UUID uniqueId) {
        profiles.remove(uniqueId);
    }

    public Collection<StaffProfile> all() {
        return profiles.values();
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (StaffProfile profile : profiles.values()) {
            String path = "players." + profile.getUniqueId();
            data.set(path + ".last-name", profile.getLastName());
            data.set(path + ".rank", profile.getRank());
            data.set(path + ".worked-seconds", profile.getWorkedSeconds());
            data.set(path + ".shift-started-at", profile.getShiftStartedAt());
            data.set(path + ".last-salary-at", profile.getLastSalaryAt());
            data.set(path + ".punishments", profile.getPunishments());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    private void load() {
        if (!dataFile.isFile()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String rawUuid : players.getKeys(false)) {
            try {
                UUID uniqueId = UUID.fromString(rawUuid);
                String path = "players." + rawUuid;
                StaffProfile profile = new StaffProfile(uniqueId, data.getString(path + ".last-name", ""));
                profile.setRank(data.getString(path + ".rank", ""));
                profile.setWorkedSeconds(data.getLong(path + ".worked-seconds"));
                profile.setShiftStartedAt(data.getLong(path + ".shift-started-at"));
                profile.setLastSalaryAt(data.getLong(path + ".last-salary-at"));
                profile.setPunishments(data.getInt(path + ".punishments"));
                profiles.put(uniqueId, profile);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid UUID in data.yml: " + rawUuid);
            }
        }
    }
}
