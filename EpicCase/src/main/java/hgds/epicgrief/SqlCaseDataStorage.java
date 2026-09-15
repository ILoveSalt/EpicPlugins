package hgds.epicgrief;

import com.google.gson.Gson;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class SqlCaseDataStorage implements CaseDataStorage {
    private final Gson gson;
    private final Connection connection;

    SqlCaseDataStorage(JavaPlugin plugin, Gson gson) throws SQLException, IOException {
        this.gson = gson;
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("sql.host", "localhost");
        int port = config.getInt("sql.port", 3306);
        String database = config.getString("sql.database", "case");
        String username = config.getString("sql.username", "root");
        String password = config.getString("sql.password", "");
        String parameters = config.getString("sql.parameters",
                "useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&autoReconnect=true");

        loadDriver();
        createDatabaseIfNeeded(host, port, database, username, password, parameters);
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + parameters,
                username,
                password
        );
        initialize();
    }

    @Override
    public PlayerCaseData load(OfflinePlayer player) throws IOException {
        String sql = "SELECT data FROM epiccase_playerdata WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return PlayerCaseData.fromJson(result.getString("data"), player, gson);
                }
            }
            return PlayerCaseData.create(player);
        } catch (SQLException exception) {
            throw new IOException("Failed to load SQL player data", exception);
        }
    }

    @Override
    public void save(PlayerCaseData data) throws IOException {
        String sql = "INSERT INTO epiccase_playerdata(uuid, name, data, updated_at) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), data = VALUES(data), updated_at = VALUES(updated_at)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, data.getName());
            statement.setString(3, data.toJson(gson));
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("Failed to save SQL player data", exception);
        }
    }

    @Override
    public void delete(OfflinePlayer player) throws IOException {
        String sql = "DELETE FROM epiccase_playerdata WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("Failed to delete SQL player data", exception);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new IOException("Failed to close SQL connection", exception);
        }
    }

    private void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS epiccase_playerdata (
                        uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                        name VARCHAR(32) NOT NULL,
                        data LONGTEXT NOT NULL,
                        updated_at BIGINT NOT NULL
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
        }
    }

    private void createDatabaseIfNeeded(
            String host,
            int port,
            String database,
            String username,
            String password,
            String parameters
    ) {
        String url = "jdbc:mysql://" + host + ":" + port + "/?" + parameters;
        try (Connection rootConnection = DriverManager.getConnection(url, username, password);
             Statement statement = rootConnection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database.replace("`", "``")
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException ignored) {
            // Some hosts forbid CREATE DATABASE; the normal database connection will report the real error if needed.
        }
    }

    private void loadDriver() throws IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IOException("MySQL JDBC driver not found. Add mysql-connector-j or enable plugin.yml libraries.", exception);
        }
    }
}
