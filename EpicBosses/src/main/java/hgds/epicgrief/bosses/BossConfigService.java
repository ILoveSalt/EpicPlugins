package hgds.epicgrief.bosses;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class BossConfigService {

    private final JavaPlugin plugin;

    private int spawnIntervalSeconds = 3600;
    private Map<String, BossDefinition> bosses = Map.of();
    private BossBarSettings bossBarSettings = new BossBarSettings("", BarColor.YELLOW, BarStyle.SOLID, 0);
    private BossBarSettings endBossBarSettings = new BossBarSettings("", BarColor.RED, BarStyle.SOLID, 10);
    private FreezeSettings freezeSettings = new FreezeSettings(5, new PotionSpec("BLINDNESS", 5, 1), "");
    private ThrowSettings throwSettings = new ThrowSettings(5, 10, "");

    public BossConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        spawnIntervalSeconds = Math.max(1, config.getInt("time", 3600));
        bossBarSettings = readBossBar(config, "settings.bossbar", 0, BarColor.YELLOW, BarStyle.SOLID);
        endBossBarSettings = readBossBar(config, "settings.bossbar-on-end", 10, BarColor.RED, BarStyle.SOLID);
        freezeSettings = readFreezeSettings(config);
        throwSettings = readThrowSettings(config);

        ConfigurationSection bossesSection = config.getConfigurationSection("bosses");
        if (bossesSection == null) {
            bosses = Map.of();
            return;
        }

        Map<String, BossDefinition> loaded = new LinkedHashMap<>();
        for (String id : bossesSection.getKeys(false)) {
            ConfigurationSection section = bossesSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            BossDefinition definition = readBoss(id, section);
            if (definition != null) {
                loaded.put(id.toLowerCase(Locale.ROOT), definition);
            }
        }

        bosses = Collections.unmodifiableMap(loaded);
    }

    public int spawnIntervalSeconds() {
        return spawnIntervalSeconds;
    }

    public Map<String, BossDefinition> bosses() {
        return bosses;
    }

    public BossDefinition boss(String id) {
        if (id == null) {
            return null;
        }

        return bosses.get(id.toLowerCase(Locale.ROOT));
    }

    public BossBarSettings bossBarSettings() {
        return bossBarSettings;
    }

    public BossBarSettings endBossBarSettings() {
        return endBossBarSettings;
    }

    public FreezeSettings freezeSettings() {
        return freezeSettings;
    }

    public ThrowSettings throwSettings() {
        return throwSettings;
    }

    private BossDefinition readBoss(String id, ConfigurationSection section) {
        String name = section.getString("name", id);
        EntityType type = readEntityType(section.getString("type"), id);
        LocationDefinition location = LocationDefinition.parse(section.getString("location"));
        if (type == null || location == null) {
            return null;
        }

        double maxHealth = Math.max(1.0D, section.getDouble("maxHealth", 20.0D));
        PushOptions push = new PushOptions(
                chance(section.getDouble("push.chance", 0.0D)),
                section.getDouble("push.x", 0.0D),
                section.getDouble("push.y", 0.0D),
                section.getDouble("push.z", 0.0D)
        );
        DamageOptions damage = new DamageOptions(
                chance(section.getDouble("damage.chance", 0.0D)),
                Math.max(0.0D, section.getDouble("damage.multiply", 1.0D))
        );

        List<String> spawnCommands = section.getStringList("spawn");
        List<String> rewardCommands = section.getStringList("reward");
        List<String> timeoutCommands = section.getStringList("timeout");
        int lifeTimeSeconds = Math.max(0, section.getInt("lifeTime", 3000));
        SpawnMobOptions spawnMob = readSpawnMob(section.getConfigurationSection("spawnMob"));
        List<ItemReward> itemRewards = readItems(section.getStringList("items"));
        TreeMap<Integer, StageOptions> stages = readStages(section.getConfigurationSection("stages"));
        List<AbilityDefinition> abilities = readAbilities(section.getStringList("effects"));

        return new BossDefinition(
                id,
                name,
                type,
                location,
                maxHealth,
                push,
                damage,
                spawnCommands,
                rewardCommands,
                timeoutCommands,
                lifeTimeSeconds,
                spawnMob,
                itemRewards,
                stages,
                abilities
        );
    }

    private BossBarSettings readBossBar(
            FileConfiguration config,
            String path,
            int defaultDeleteTime,
            BarColor defaultColor,
            BarStyle defaultStyle
    ) {
        String title = config.getString(path + ".title", "");
        BarColor color = readEnum(BarColor.class, config.getString(path + ".color"), defaultColor);
        BarStyle style = readEnum(BarStyle.class, config.getString(path + ".style"), defaultStyle);
        int deleteTime = Math.max(0, config.getInt(path + ".time-to-delete", defaultDeleteTime));
        return new BossBarSettings(title, color, style, deleteTime);
    }

    private FreezeSettings readFreezeSettings(FileConfiguration config) {
        int time = Math.max(1, config.getInt("settings.effects.freeze.time", 5));
        PotionSpec potion = PotionSpec.parse(config.getString("settings.effects.freeze.effect", "BLINDNESS;5;1"));
        String title = config.getString("settings.effects.freeze.title", "");
        return new FreezeSettings(time, potion, title);
    }

    private ThrowSettings readThrowSettings(FileConfiguration config) {
        double min = Math.max(0.0D, config.getDouble("settings.effects.throw.min", 5.0D));
        double max = Math.max(min, config.getDouble("settings.effects.throw.max", 10.0D));
        String title = config.getString("settings.effects.throw.title", "");
        return new ThrowSettings(min, max, title);
    }

    private SpawnMobOptions readSpawnMob(ConfigurationSection section) {
        if (section == null) {
            return new SpawnMobOptions(0, List.of());
        }

        int interval = Math.max(0, section.getInt("time", 0));
        List<MinionDefinition> minions = new ArrayList<>();
        for (String raw : section.getStringList("types")) {
            MinionDefinition minion = MinionDefinition.parse(raw);
            if (minion != null) {
                minions.add(minion);
            }
        }

        return new SpawnMobOptions(interval, minions);
    }

    private List<ItemReward> readItems(List<String> rawItems) {
        List<ItemReward> rewards = new ArrayList<>();
        for (String raw : rawItems) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String[] parts = raw.split(";");
            Material material = readEnum(Material.class, parts[0], null);
            if (material == null || material.isAir()) {
                plugin.getLogger().warning("Skipping invalid boss item: " + raw);
                continue;
            }

            int amount = parts.length >= 2 ? parseInt(parts[1], 1) : 1;
            double chance = parts.length >= 3 ? chance(parseDouble(parts[2], 100.0D)) : 100.0D;
            rewards.add(new ItemReward(material, Math.max(1, amount), chance));
        }

        return rewards;
    }

    private TreeMap<Integer, StageOptions> readStages(ConfigurationSection section) {
        TreeMap<Integer, StageOptions> stages = new TreeMap<>();
        if (section == null) {
            return stages;
        }

        for (String rawThreshold : section.getKeys(false)) {
            int threshold = parseInt(rawThreshold, -1);
            if (threshold < 0 || threshold > 100) {
                plugin.getLogger().warning("Skipping invalid boss stage percent: " + rawThreshold);
                continue;
            }

            ConfigurationSection stageSection = section.getConfigurationSection(rawThreshold);
            if (stageSection == null) {
                continue;
            }

            double speed = stageSection.getDouble("speed", -1.0D);
            double damage = stageSection.getDouble("damage", -1.0D);
            stages.put(threshold, new StageOptions(speed, damage));
        }

        return stages;
    }

    private List<AbilityDefinition> readAbilities(List<String> rawAbilities) {
        List<AbilityDefinition> abilities = new ArrayList<>();
        for (String raw : rawAbilities) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String[] parts = raw.split(";");
            if (parts.length < 2) {
                plugin.getLogger().warning("Skipping invalid boss effect: " + raw);
                continue;
            }

            String type = parts[0].trim().toUpperCase(Locale.ROOT);
            String[] range = parts[1].split("-");
            int min = parseInt(range[0], 30);
            int max = range.length >= 2 ? parseInt(range[1], min) : min;
            if (min > max) {
                int oldMin = min;
                min = max;
                max = oldMin;
            }

            abilities.add(new AbilityDefinition(type, Math.max(1, min), Math.max(1, max)));
        }

        return abilities;
    }

    private EntityType readEntityType(String raw, String id) {
        EntityType type = readEnum(EntityType.class, raw, null);
        if (type == null || !type.isAlive()) {
            plugin.getLogger().warning("Boss " + id + " has invalid entity type: " + raw);
            return null;
        }

        return type;
    }

    private <T extends Enum<T>> T readEnum(Class<T> enumType, String raw, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private double chance(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
