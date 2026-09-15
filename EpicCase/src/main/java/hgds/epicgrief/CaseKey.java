package hgds.epicgrief;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CaseKey {
    private final String id;
    private final String name;
    private final String itemSpec;
    private final List<String> description;
    private final Set<String> canOpen;

    private CaseKey(String id, String name, String itemSpec, List<String> description, Set<String> canOpen) {
        this.id = id;
        this.name = name;
        this.itemSpec = itemSpec;
        this.description = List.copyOf(description);
        this.canOpen = Set.copyOf(canOpen);
    }

    static CaseKey fromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = stripExtension(file.getName()).toLowerCase(Locale.ROOT);
        Set<String> canOpen = new LinkedHashSet<>();
        for (String boxId : config.getStringList("canOpen")) {
            String normalized = normalize(boxId);
            if (!normalized.isBlank()) {
                canOpen.add(normalized);
            }
        }
        return new CaseKey(
                id,
                config.getString("name", id),
                config.getString("item", "TRIPWIRE_HOOK"),
                config.getStringList("description"),
                canOpen
        );
    }

    boolean canOpen(String boxId) {
        return canOpen.contains(normalize(boxId));
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getItemSpec() {
        return itemSpec;
    }

    List<String> getDescription() {
        return description;
    }

    Set<String> getCanOpen() {
        return canOpen;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }
}
