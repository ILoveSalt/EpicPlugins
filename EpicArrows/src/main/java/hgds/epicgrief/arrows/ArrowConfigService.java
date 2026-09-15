package hgds.epicgrief.arrows;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArrowConfigService {

    private final JavaPlugin plugin;
    private final File arrowsFile;

    private Map<String, ArrowDefinition> arrowsByKey = Map.of();
    private Map<UUID, ArrowDefinition> arrowsByUuid = Map.of();
    private Set<String> disabledWorlds = Set.of();
    private Set<String> disabledArrows = Set.of();

    public ArrowConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.arrowsFile = new File(plugin.getDataFolder(), "arrows.yml");
    }

    public void reload() {
        loadDisabledWorlds();
        loadDisabledArrows();
        loadArrows();
    }

    public Collection<ArrowDefinition> arrows() {
        return Collections.unmodifiableCollection(arrowsByKey.values());
    }

    public ArrowDefinition arrow(String key) {
        if (key == null) {
            return null;
        }

        return arrowsByKey.get(key.toLowerCase(Locale.ROOT));
    }

    public ArrowDefinition arrow(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        return arrowsByUuid.get(uuid);
    }

    public List<String> arrowKeys() {
        return new ArrayList<>(arrowsByKey.keySet());
    }

    public boolean isDisabledWorld(String worldName) {
        if (worldName == null) {
            return false;
        }

        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public boolean isDisabledArrow(ArrowDefinition arrow) {
        return arrow != null && disabledArrows.contains(arrow.key().toLowerCase(Locale.ROOT));
    }

    public boolean isDisabledArrow(String key) {
        return key != null && disabledArrows.contains(key.toLowerCase(Locale.ROOT));
    }

    public List<String> disabledArrowKeys() {
        return new ArrayList<>(disabledArrows);
    }

    public boolean disableArrow(String key) {
        ArrowDefinition arrow = arrow(key);
        if (arrow == null) {
            return false;
        }

        Set<String> arrows = new HashSet<>(disabledArrows);
        if (!arrows.add(arrow.key())) {
            return true;
        }

        saveDisabledArrows(arrows);
        return true;
    }

    public boolean enableArrow(String key) {
        ArrowDefinition arrow = arrow(key);
        if (arrow == null) {
            return false;
        }

        Set<String> arrows = new HashSet<>(disabledArrows);
        if (!arrows.remove(arrow.key())) {
            return true;
        }

        saveDisabledArrows(arrows);
        return true;
    }

    private void loadDisabledWorlds() {
        Set<String> worlds = new HashSet<>();
        addNormalized(plugin.getConfig().getStringList("disable-worlds"), worlds);
        addNormalized(plugin.getConfig().getStringList("disabledWorlds"), worlds);
        disabledWorlds = Set.copyOf(worlds);
    }

    private void loadDisabledArrows() {
        Set<String> arrows = new HashSet<>();
        addNormalized(plugin.getConfig().getStringList("arrows_disable"), arrows);
        disabledArrows = Set.copyOf(arrows);
    }

    private void loadArrows() {
        YamlConfiguration arrowsConfig = YamlConfiguration.loadConfiguration(arrowsFile);
        ConfigurationSection idSection = arrowsConfig.getConfigurationSection("arrows");
        if (idSection == null) {
            idSection = arrowsConfig.createSection("arrows");
        }

        ConfigurationSection configSection = plugin.getConfig().getConfigurationSection("arrows");
        Map<String, ArrowDefinition> byKey = new LinkedHashMap<>();
        Map<UUID, ArrowDefinition> byUuid = new HashMap<>();
        boolean dirty = false;

        if (configSection != null) {
            for (String rawKey : configSection.getKeys(false)) {
                String key = rawKey.toLowerCase(Locale.ROOT);
                ConfigurationSection section = configSection.getConfigurationSection(rawKey);
                if (section == null) {
                    continue;
                }

                UUID uuid = readOrCreateUuid(idSection, rawKey);
                if (!idSection.contains(rawKey)) {
                    idSection.set(rawKey, uuid.toString());
                    dirty = true;
                }

                ArrowDefinition arrow = parseArrow(key, uuid, section);
                byKey.put(key, arrow);
                byUuid.put(uuid, arrow);
            }
        }

        if (dirty) {
            saveArrowsConfig(arrowsConfig);
        }

        arrowsByKey = Map.copyOf(byKey);
        arrowsByUuid = Map.copyOf(byUuid);
    }

    private UUID readOrCreateUuid(ConfigurationSection section, String key) {
        String rawUuid = section.getString(key, "");
        if (rawUuid != null && !rawUuid.isBlank()) {
            try {
                return UUID.fromString(rawUuid);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid UUID for arrow " + key + " in arrows.yml. A new UUID will be used.");
            }
        }

        return UUID.randomUUID();
    }

    private ArrowDefinition parseArrow(String key, UUID uuid, ConfigurationSection section) {
        ConfigurationSection item = section.getConfigurationSection("item");
        Material material = parseMaterial(item == null ? null : item.getString("type"), Material.TIPPED_ARROW);
        int amount = item == null ? 1 : Math.max(1, item.getInt("amount", 1));
        String title = item == null ? "" : item.getString("title", "");
        List<String> lore = item == null ? List.of() : item.getStringList("lore");
        Color color = parseColor(item == null ? null : item.getString("arrow-color"));
        List<String> flags = item == null ? List.of() : item.getStringList("flags");
        List<PotionEffect> effects = item == null ? List.of() : parsePotionEffects(item.getStringList("arrow-effects"));

        ArrowType type = ArrowType.from(section.getString("arrow-type"));
        ParticleOptions particle = parseParticleOptions(section.getString("arrow-settings", ""));
        double aimRadius = Math.max(1.0D, section.getDouble("arrow-settings-aim", 10.0D));
        ExplosionOptions explosion = parseExplosionOptions(section.getString("arrow-settings-explode", "4 false false"));
        TeleportOptions teleport = parseTeleportOptions(section.getString("arrow-settings-teleport", ""));
        IceOptions ice = parseIceOptions(section.getString("arrow-settings-ice", "5 1"));

        return new ArrowDefinition(
                key,
                uuid,
                title,
                material,
                amount,
                lore,
                color,
                flags,
                effects,
                type,
                particle,
                aimRadius,
                explosion,
                teleport,
                ice
        );
    }

    private ParticleOptions parseParticleOptions(String value) {
        String[] parts = split(value);
        Particle particle = parts.length >= 1 ? parseParticle(parts[0]) : null;
        boolean enabled = parts.length >= 2 ? Boolean.parseBoolean(parts[1]) : particle != null;
        return new ParticleOptions(particle, enabled);
    }

    private ExplosionOptions parseExplosionOptions(String value) {
        String[] parts = split(value);
        float power = parts.length >= 1 ? parseFloat(parts[0], 4.0F) : 4.0F;
        boolean setFire = parts.length >= 2 && Boolean.parseBoolean(parts[1]);
        boolean breakBlocks = parts.length >= 3 && Boolean.parseBoolean(parts[2]);
        return new ExplosionOptions(power, setFire, breakBlocks);
    }

    private TeleportOptions parseTeleportOptions(String value) {
        String[] parts = split(value);
        Sound sound = parts.length >= 1 ? parseSound(parts[0]) : null;
        Material blockedMaterial = parts.length >= 2 ? parseMaterial(parts[1], null) : null;
        return new TeleportOptions(sound, blockedMaterial);
    }

    private IceOptions parseIceOptions(String value) {
        String[] parts = split(value);
        int seconds = parts.length >= 1 ? Math.max(1, parseInt(parts[0], 5)) : 5;
        int amplifier = parts.length >= 2 ? Math.max(0, parseInt(parts[1], 1)) : 1;
        return new IceOptions(seconds, amplifier);
    }

    private List<PotionEffect> parsePotionEffects(List<String> rawEffects) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String rawEffect : rawEffects) {
            if (rawEffect == null || rawEffect.isBlank()) {
                continue;
            }

            String[] parts = rawEffect.split(":");
            if (parts.length < 3) {
                plugin.getLogger().warning("Invalid potion effect format: " + rawEffect);
                continue;
            }

            PotionEffectType type = parsePotionEffectType(parts[0]);
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect: " + parts[0]);
                continue;
            }

            int duration = Math.max(1, parseInt(parts[1], 20));
            int amplifier = Math.max(0, parseInt(parts[2], 0));
            effects.add(new PotionEffect(type, duration, amplifier));
        }

        return List.copyOf(effects);
    }

    private PotionEffectType parsePotionEffectType(String name) {
        for (String candidate : legacyEffectCandidates(name)) {
            PotionEffectType type = PotionEffectType.getByName(candidate);
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    private List<String> legacyEffectCandidates(String name) {
        String normalized = normalizeName(name);
        if ("INCREASE_DAMAGE".equals(normalized)) {
            return List.of("INCREASE_DAMAGE", "STRENGTH");
        }
        if ("SLOW".equals(normalized)) {
            return List.of("SLOW", "SLOWNESS");
        }
        if ("JUMP".equals(normalized)) {
            return List.of("JUMP", "JUMP_BOOST");
        }

        return List.of(normalized);
    }

    private Particle parseParticle(String name) {
        for (String candidate : legacyParticleCandidates(name)) {
            try {
                return Particle.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // Try the next known Bukkit/Paper name.
            }
        }

        plugin.getLogger().warning("Unknown particle: " + name);
        return null;
    }

    private List<String> legacyParticleCandidates(String name) {
        String normalized = normalizeName(name);
        if ("FIREWORKS_SPARK".equals(normalized)) {
            return List.of("FIREWORKS_SPARK", "FIREWORK");
        }
        if ("EXPLOSION_NORMAL".equals(normalized)) {
            return List.of("EXPLOSION_NORMAL", "EXPLOSION");
        }
        if ("SPELL".equals(normalized)) {
            return List.of("SPELL", "EFFECT");
        }

        return List.of(normalized);
    }

    private Sound parseSound(String name) {
        String normalized = normalizeName(name);
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown sound: " + name);
            return null;
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(normalizeName(name));
        if (material == null) {
            plugin.getLogger().warning("Unknown material: " + name);
            return fallback;
        }

        return material;
    }

    private Color parseColor(String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return null;
        }

        String value = rawColor.startsWith("#") ? rawColor.substring(1) : rawColor;
        try {
            return Color.fromRGB(Integer.parseInt(value, 16));
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid arrow color: " + rawColor);
            return null;
        }
    }

    private void saveArrowsConfig(YamlConfiguration arrowsConfig) {
        try {
            arrowsConfig.save(arrowsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save arrows.yml: " + exception.getMessage());
        }
    }

    private void addNormalized(List<String> values, Set<String> target) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    private String[] split(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }

        return value.trim().split("\\s+");
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void saveDisabledArrows(Set<String> arrows) {
        List<String> sortedArrows = new ArrayList<>(arrows);
        Collections.sort(sortedArrows);
        plugin.getConfig().set("arrows_disable", sortedArrows);
        plugin.saveConfig();
        disabledArrows = Set.copyOf(arrows);
    }
}
