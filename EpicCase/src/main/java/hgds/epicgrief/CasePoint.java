package hgds.epicgrief;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CasePoint {
    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private String selector;
    private String openByDefault;
    private Set<String> available;
    private boolean showOnlyAvailable;
    private double hologramHeight;
    private String hologramType;
    private String hologramDirection;
    private List<String> hologramLines;
    private List<String> effectorKit;
    private Map<String, Object> parameters;

    private CasePoint(
            String id,
            String world,
            int x,
            int y,
            int z,
            String selector,
            String openByDefault,
            Set<String> available,
            boolean showOnlyAvailable,
            double hologramHeight,
            String hologramType,
            String hologramDirection,
            List<String> hologramLines,
            List<String> effectorKit,
            Map<String, Object> parameters
    ) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.selector = selector;
        this.openByDefault = normalizeBoxId(openByDefault);
        this.available = new LinkedHashSet<>(available);
        this.showOnlyAvailable = showOnlyAvailable;
        this.hologramHeight = hologramHeight;
        this.hologramType = hologramType == null || hologramType.isBlank() ? "normal" : hologramType.toLowerCase(Locale.ROOT);
        this.hologramDirection = hologramDirection == null || hologramDirection.isBlank() ? "NORTH" : hologramDirection.toUpperCase(Locale.ROOT);
        this.hologramLines = new ArrayList<>(hologramLines);
        this.effectorKit = new ArrayList<>(effectorKit);
        this.parameters = new LinkedHashMap<>(parameters);
    }

    static CasePoint fromBlock(String id, Block block) {
        return new CasePoint(
                id,
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ(),
                "INVENTORY",
                "",
                new LinkedHashSet<>(),
                false,
                3.0D,
                "normal",
                "NORTH",
                List.of(
                        "t:&c&l! &6&lCASES &c&l!",
                        "t:",
                        "t:&4&l>> &fClick to open cases &4&l<<"
                ),
                List.of(),
                Map.of()
        );
    }

    static CasePoint fromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String fallbackId = stripExtension(file.getName());
        String id = config.getString("id", fallbackId);
        String world = config.getString("location.world", config.getString("world", ""));
        int x = config.getInt("location.x", config.getInt("x"));
        int y = config.getInt("location.y", config.getInt("y"));
        int z = config.getInt("location.z", config.getInt("z"));
        String selector = config.getString("selector", "INVENTORY");
        String openByDefault = config.getString("openByDefault", "");
        boolean showOnlyAvailable = config.getBoolean("showOnlyAvailable", false);
        double hologramHeight = config.getDouble("hologram.height", config.getDouble("hologramHeight", 3.0D));
        String hologramType = config.getString("hologram.type", "normal");
        String hologramDirection = config.getString("hologram.direction", "NORTH");
        List<String> hologramLines = config.getStringList("hologram.lines");
        if (hologramLines.isEmpty()) {
            hologramLines = config.getStringList("lines");
        }
        List<String> effectorKit = config.getStringList("effectorKit");
        if (effectorKit.isEmpty()) {
            effectorKit = config.getStringList("effectors");
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (config.isConfigurationSection("params")) {
            parameters.putAll(config.getConfigurationSection("params").getValues(true));
        }

        Set<String> available = new LinkedHashSet<>();
        for (String boxId : config.getStringList("available")) {
            String normalized = normalizeBoxId(boxId);
            if (!normalized.isBlank()) {
                available.add(normalized);
            }
        }

        return new CasePoint(
                id,
                world,
                x,
                y,
                z,
                selector,
                openByDefault,
                available,
                showOnlyAvailable,
                hologramHeight,
                hologramType,
                hologramDirection,
                hologramLines,
                effectorKit,
                parameters
        );
    }

    void save(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", id);
        config.set("location.world", world);
        config.set("location.x", x);
        config.set("location.y", y);
        config.set("location.z", z);
        config.set("selector", selector);
        config.set("openByDefault", openByDefault == null ? "" : openByDefault);
        config.set("available", new ArrayList<>(available));
        config.set("showOnlyAvailable", showOnlyAvailable);
        config.set("hologram.height", hologramHeight);
        config.set("hologram.type", hologramType);
        config.set("hologram.direction", hologramDirection);
        config.set("hologram.lines", new ArrayList<>(hologramLines));
        config.set("effectorKit", new ArrayList<>(effectorKit));
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            config.set("params." + entry.getKey(), entry.getValue());
        }
        config.save(file);
    }

    boolean allowsBox(String boxId) {
        if (available.isEmpty()) {
            return true;
        }
        return available.contains(normalizeBoxId(boxId));
    }

    String blockKey() {
        return blockKey(world, x, y, z);
    }

    static String blockKey(Block block) {
        return blockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    static String blockKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    static String normalizeBoxId(String boxId) {
        if (boxId == null) {
            return "";
        }
        return boxId.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    String getId() {
        return id;
    }

    String getOpenByDefault() {
        return openByDefault == null ? "" : openByDefault;
    }

    String getHologramDirection() {
        return hologramDirection;
    }

    String getWorld() {
        return world;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getZ() {
        return z;
    }

    void setOpenByDefault(String openByDefault) {
        this.openByDefault = normalizeBoxId(openByDefault);
    }

    Set<String> getAvailable() {
        return available;
    }

    void setAvailable(List<String> boxIds) {
        available = new LinkedHashSet<>();
        for (String boxId : boxIds) {
            String normalized = normalizeBoxId(boxId);
            if (!normalized.isBlank()) {
                available.add(normalized);
            }
        }
    }

    boolean isShowOnlyAvailable() {
        return showOnlyAvailable;
    }

    void setShowOnlyAvailable(boolean showOnlyAvailable) {
        this.showOnlyAvailable = showOnlyAvailable;
    }

    void setSelector(String selector) {
        this.selector = selector == null || selector.isBlank() ? "INVENTORY" : selector.toUpperCase(Locale.ROOT);
    }

    String getSelector() {
        return selector;
    }

    void setHologramHeight(double hologramHeight) {
        this.hologramHeight = hologramHeight;
    }

    double getHologramHeight() {
        return hologramHeight;
    }

    void setHologramType(String hologramType) {
        this.hologramType = hologramType == null || hologramType.isBlank()
                ? "normal"
                : hologramType.toLowerCase(Locale.ROOT);
    }

    String getHologramType() {
        return hologramType;
    }

    List<String> getHologramLines() {
        return hologramLines;
    }

    void setHologramLine(int index, String line) {
        while (hologramLines.size() <= index) {
            hologramLines.add("");
        }
        hologramLines.set(index, line);
    }

    void removeHologramLine(int index) {
        if (index >= 0 && index < hologramLines.size()) {
            hologramLines.remove(index);
        }
    }

    List<String> getEffectorKit() {
        return List.copyOf(effectorKit);
    }

    void setEffectorKit(List<String> effectors) {
        effectorKit = effectors.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }

    void setParameter(String key, Object value) {
        if (value == null) {
            parameters.remove(key);
        } else {
            parameters.put(key, value);
        }
    }

    Settings copySettings() {
        return new Settings(
                selector,
                openByDefault,
                new ArrayList<>(available),
                showOnlyAvailable,
                hologramHeight,
                hologramType,
                hologramDirection,
                new ArrayList<>(hologramLines),
                new ArrayList<>(effectorKit),
                new LinkedHashMap<>(parameters)
        );
    }

    void applySettings(Settings settings) {
        selector = settings.selector();
        openByDefault = settings.openByDefault();
        available = new LinkedHashSet<>(settings.available());
        showOnlyAvailable = settings.showOnlyAvailable();
        hologramHeight = settings.hologramHeight();
        hologramType = settings.hologramType();
        hologramLines = new ArrayList<>(settings.hologramLines());
        effectorKit = new ArrayList<>(settings.effectorKit());
        parameters = new LinkedHashMap<>(settings.parameters());
    }

    record Settings(
            String selector,
            String openByDefault,
            List<String> available,
            boolean showOnlyAvailable,
            double hologramHeight,
            String hologramType,
            String hologramDirection,
            List<String> hologramLines,
            List<String> effectorKit,
            Map<String, Object> parameters
    ) {
    }
}
