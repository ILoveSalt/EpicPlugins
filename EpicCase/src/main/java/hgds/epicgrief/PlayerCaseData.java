package hgds.epicgrief;

import com.google.gson.Gson;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class PlayerCaseData {
    private UUID uuid;
    private String name;
    private Map<String, Integer> cases = new LinkedHashMap<>();
    private Map<String, Integer> keys = new LinkedHashMap<>();
    private int opened;
    private long lastAward;
    private List<HistoryEntry> history = new ArrayList<>();

    static PlayerCaseData create(OfflinePlayer player) {
        PlayerCaseData data = new PlayerCaseData();
        data.uuid = player.getUniqueId();
        data.name = player.getName() == null || player.getName().isBlank()
                ? player.getUniqueId().toString()
                : player.getName();
        data.ensureDefaults();
        return data;
    }

    static PlayerCaseData load(File folder, OfflinePlayer player, Gson gson) throws IOException {
        Files.createDirectories(folder.toPath());
        File file = file(folder, player.getUniqueId());
        PlayerCaseData data;

        if (file.isFile()) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                data = gson.fromJson(reader, PlayerCaseData.class);
            }
            if (data == null) {
                data = new PlayerCaseData();
            }
        } else {
            data = create(player);
        }

        return prepare(data, player);
    }

    void save(File folder, Gson gson) throws IOException {
        ensureDefaults();
        Files.createDirectories(folder.toPath());
        File file = file(folder, uuid);
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        }
    }

    static PlayerCaseData fromJson(String json, OfflinePlayer player, Gson gson) {
        PlayerCaseData data = gson.fromJson(json, PlayerCaseData.class);
        if (data == null) {
            data = create(player);
        }
        return prepare(data, player);
    }

    String toJson(Gson gson) {
        ensureDefaults();
        return gson.toJson(this);
    }

    static File file(File folder, UUID uuid) {
        return new File(folder, uuid + ".json");
    }

    int getCases(String boxId) {
        return cases.getOrDefault(normalize(boxId), 0);
    }

    int getKeys(String boxId) {
        return keys.getOrDefault(normalize(boxId), 0);
    }

    int addCases(String boxId, int amount) {
        return setCases(boxId, getCases(boxId) + amount);
    }

    int addKeys(String boxId, int amount) {
        return setKeys(boxId, getKeys(boxId) + amount);
    }

    int setCases(String boxId, int amount) {
        String normalized = normalize(boxId);
        int safeAmount = Math.max(0, amount);
        if (safeAmount == 0) {
            cases.remove(normalized);
        } else {
            cases.put(normalized, safeAmount);
        }
        return safeAmount;
    }

    int setKeys(String boxId, int amount) {
        String normalized = normalize(boxId);
        int safeAmount = Math.max(0, amount);
        if (safeAmount == 0) {
            keys.remove(normalized);
        } else {
            keys.put(normalized, safeAmount);
        }
        return safeAmount;
    }

    void addHistory(CaseBox box, CaseAward award, String key) {
        opened++;
        lastAward = System.currentTimeMillis();
        history.add(0, new HistoryEntry(
                lastAward,
                box.getId(),
                award.getId(),
                award.getHologramText(),
                award.getChance(),
                key
        ));
        while (history.size() > 50) {
            history.remove(history.size() - 1);
        }
    }

    private void ensureDefaults() {
        if (cases == null) {
            cases = new LinkedHashMap<>();
        }
        if (keys == null) {
            keys = new LinkedHashMap<>();
        }
        if (history == null) {
            history = new ArrayList<>();
        }
    }

    private static PlayerCaseData prepare(PlayerCaseData data, OfflinePlayer player) {
        data.uuid = player.getUniqueId();
        if (player.getName() != null && !player.getName().isBlank()) {
            data.name = player.getName();
        } else if (data.name == null || data.name.isBlank()) {
            data.name = player.getUniqueId().toString();
        }
        data.ensureDefaults();
        return data;
    }

    private static String normalize(String boxId) {
        if (boxId == null) {
            return "";
        }
        return boxId.trim().toLowerCase(Locale.ROOT);
    }

    UUID getUuid() {
        return uuid;
    }

    String getName() {
        return name;
    }

    int getOpened() {
        return opened;
    }

    List<HistoryEntry> getHistory() {
        ensureDefaults();
        return List.copyOf(history);
    }

    static final class HistoryEntry {
        private final long time;
        private final String box;
        private final String award;
        private final String text;
        private final double chance;
        private final String key;

        private HistoryEntry(long time, String box, String award, String text, double chance, String key) {
            this.time = time;
            this.box = box;
            this.award = award;
            this.text = text;
            this.chance = chance;
            this.key = key;
        }

        long getTime() {
            return time;
        }

        String getBox() {
            return box;
        }

        String getAward() {
            return award;
        }

        String getText() {
            return text;
        }

        double getChance() {
            return chance;
        }

        String getKey() {
            return key == null ? "" : key;
        }
    }
}
