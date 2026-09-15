package hgds.epicgrief;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CaseCatalog {
    private final Map<String, CaseBox> boxes = new LinkedHashMap<>();
    private final Map<String, CaseKey> keys = new LinkedHashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    void reload(File boxesFolder, File keysFolder, Logger logger) {
        boxes.clear();
        keys.clear();
        loadErrors.clear();
        loadFolder(boxesFolder, logger, file -> {
            CaseBox box = CaseBox.fromFile(file);
            boxes.put(box.getId(), box);
        });
        loadFolder(keysFolder, logger, file -> {
            CaseKey key = CaseKey.fromFile(file);
            keys.put(key.getId(), key);
        });
    }

    List<CaseKey> keysFor(CaseBox box) {
        return keys.values().stream().filter(key -> key.canOpen(box.getId())).toList();
    }

    Map<String, CaseBox> boxes() {
        return boxes;
    }

    Map<String, CaseKey> keys() {
        return keys;
    }

    List<String> loadErrors() {
        return List.copyOf(loadErrors);
    }

    private void loadFolder(File folder, Logger logger, FileLoader loader) {
        File[] files = folder.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                loader.load(file);
            } catch (RuntimeException exception) {
                String error = file.getName() + ": " + exception.getMessage();
                loadErrors.add(error);
                logger.log(Level.WARNING, "Failed to load unit file " + file.getName(), exception);
            }
        }
    }

    @FunctionalInterface
    private interface FileLoader {
        void load(File file);
    }
}
