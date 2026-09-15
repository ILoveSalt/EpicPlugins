package hgds.epicgrief.utils;

import java.util.List;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hgds.epicgrief.utils.actionbar.Actionbar;

/**
 * Утилиты: доступ к конфигу, форматирование времени и отправка сообщений
 * (чат / title / actionbar через Adventure API).
 */
public final class Utils {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static JavaPlugin plugin;
    private static FileConfiguration config;

    private Utils() {
    }

    public static void init(JavaPlugin instance) {
        plugin = instance;
        reloadConfig();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void reloadConfig() {
        config = Config.getData("config.yml");
    }

    public static String getMessage(String path) {
        String value = config.getString("messages." + path);
        return value != null ? value : "";
    }

    public static String getString(String path) {
        String value = config.getString(path);
        return value != null ? value : "";
    }

    public static int getInt(String path) {
        return config.getInt(path);
    }

    /** Проверка права с отправкой сообщения об отказе. */
    public static boolean has(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sendMessage(sender, getMessage("no-permission"));
            return false;
        }
        return true;
    }

    /** Форматирует время вида "3 дня 2 часа 5 минут". */
    public static String format(int time) {
        int days = time / 86400;
        int hours = time % 86400 / 3600;
        int minutes = time % 3600 / 60;
        int seconds = time % 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(getString("time.days").replace("%size%", String.valueOf(days))).append(' ');
        }
        if (hours > 0) {
            builder.append(getString("time.hours").replace("%size%", String.valueOf(hours))).append(' ');
        }
        if (minutes > 0) {
            builder.append(getString("time.minutes").replace("%size%", String.valueOf(minutes))).append(' ');
        }
        if (seconds > 0) {
            builder.append(getString("time.seconds").replace("%size%", String.valueOf(seconds))).append(' ');
        }
        String result = builder.toString().trim();
        if (result.isEmpty()) {
            result = getString("time.now");
        }
        return color(result);
    }

    /** Переводит '&'-коды в legacy-символ '§' (для строкового уровня). */
    public static String color(String text) {
        return text == null ? "" : text.replace('&', '§');
    }

    /** Компонент Adventure из строки с '&'-кодами. */
    public static Component colored(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(text);
    }

    public static List<Component> colored(List<String> text) {
        return text.stream().map(Utils::colored).collect(Collectors.toList());
    }

    /**
     * Отправляет сообщение. Строки разделяются символом ';'.
     * Строка "title:<текст>" — титул (подзаголовок после %nl%),
     * "actionbar:<текст>" — экшнбар, остальное — сообщение в чат с префиксом.
     * Поддерживается плейсхолдер %player%.
     */
    public static void sendMessage(CommandSender sender, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String prefix = getMessage("prefix");
        for (String line : text.split(";")) {
            line = line.replace("%player%", sender.getName());
            if (line.startsWith("title:")) {
                if (sender instanceof Player player) {
                    Title.sendTitle(player, line.substring("title:".length()));
                }
            } else if (line.startsWith("actionbar:")) {
                if (sender instanceof Player player) {
                    Actionbar.sendActionbar(player, line.substring("actionbar:".length()));
                }
            } else {
                sender.sendMessage(colored(prefix + line));
            }
        }
    }
}
