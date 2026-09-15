package hgds.epicgrief;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class EpicLimitConnect extends JavaPlugin implements Listener {

    private static final int DEFAULT_LIMIT = 2;

    private int limit;
    private String disconnectMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("IP connection limit: " + limit);
    }

    private void loadSettings() {
        limit = getConfig().getInt("limit", DEFAULT_LIMIT);
        if (limit < 1) {
            getLogger().warning("The 'limit' value must be at least 1. Using " + DEFAULT_LIMIT + ".");
            limit = DEFAULT_LIMIT;
        }

        disconnectMessage = getConfig().getString(
                "disconnectMessage",
                "&cС одного IP одновременно может играть не более {limit} игроков."
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginValidation(PlayerConnectionValidateLoginEvent event) {
        if (!event.isAllowed()) {
            return;
        }

        InetAddress joiningAddress = event.getConnection().getClientAddress().getAddress();
        long connectedPlayers = Bukkit.getOnlinePlayers().stream()
                .map(Player::getAddress)
                .filter(address -> address != null)
                .map(InetSocketAddress::getAddress)
                .filter(joiningAddress::equals)
                .count();

        if (connectedPlayers >= limit) {
            Component message = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(disconnectMessage.replace("{limit}", Integer.toString(limit)));
            event.kickMessage(message);
        }
    }
}
