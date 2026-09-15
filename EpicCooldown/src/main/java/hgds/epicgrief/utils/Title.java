package hgds.epicgrief.utils;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

/**
 * Отправка тайтлов через Adventure API (Paper 26.1.2), без NMS-рефлексии.
 * Поддерживает %nl% для разделения заголовка и подзаголовка.
 */
public final class Title {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int DEFAULT_FADE_IN = 15;
    private static final int DEFAULT_STAY = 60;
    private static final int DEFAULT_FADE_OUT = 15;

    private Title() {
    }

    public static void sendTitle(Player player, String text) {
        sendTitle(player, text, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    public static void sendTitle(Player player, String text, int fadeIn, int stay, int fadeOut) {
        if (player == null || text == null || text.isEmpty()) {
            return;
        }
        String[] parts = text.split("%nl%");
        Component title = colored(parts[0]);
        Component subtitle = parts.length > 1 ? colored(parts[1]) : Component.empty();
        net.kyori.adventure.title.Title.Times times =
                net.kyori.adventure.title.Title.Times.times(toDuration(fadeIn), toDuration(stay), toDuration(fadeOut));
        player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle, times));
    }

    private static Duration toDuration(int ticks) {
        return Duration.ofMillis(Math.max(ticks, 0) * 50L);
    }

    private static Component colored(String legacy) {
        return LEGACY.deserialize(legacy);
    }
}
