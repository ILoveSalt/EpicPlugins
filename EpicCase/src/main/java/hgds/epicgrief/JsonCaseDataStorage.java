package hgds.epicgrief;

import com.google.gson.Gson;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class JsonCaseDataStorage implements CaseDataStorage {
    private final File folder;
    private final Gson gson;

    JsonCaseDataStorage(File folder, Gson gson) {
        this.folder = folder;
        this.gson = gson;
    }

    @Override
    public PlayerCaseData load(OfflinePlayer player) throws IOException {
        return PlayerCaseData.load(folder, player, gson);
    }

    @Override
    public void save(PlayerCaseData data) throws IOException {
        data.save(folder, gson);
    }

    @Override
    public void delete(OfflinePlayer player) throws IOException {
        Files.deleteIfExists(PlayerCaseData.file(folder, player.getUniqueId()).toPath());
    }

    @Override
    public void close() {
    }
}
