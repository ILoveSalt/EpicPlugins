package hgds.epicgrief.utils.actionbar;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

/**
 * Отправка сообщений на экшнбар через Adventure API (Paper 26.1.2),
 * без NMS-рефлексии.
 */
public final class Actionbar {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Actionbar() {
    }

    public static void sendActionbar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendActionBar(LEGACY.deserialize(message));
    }
}
