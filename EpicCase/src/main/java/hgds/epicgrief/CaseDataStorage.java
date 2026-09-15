package hgds.epicgrief;

import org.bukkit.OfflinePlayer;

import java.io.IOException;

interface CaseDataStorage extends AutoCloseable {
    PlayerCaseData load(OfflinePlayer player) throws IOException;

    void save(PlayerCaseData data) throws IOException;

    void delete(OfflinePlayer player) throws IOException;

    @Override
    void close() throws IOException;
}
