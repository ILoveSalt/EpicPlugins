package hgds.epicgrief.utils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

/**
 * Сопоставление введённых игроком команд (и их алиасов, включая "плагин:команда")
 * с каноническими именами команд через command map сервера.
 */
public final class CommandResolver {

    /** Минимальный интервал (мс) между обновлениями карты команд. */
    private static final long REBUILD_INTERVAL_MS = 30_000L;

    private final Map<String, String> aliasToCommand = new ConcurrentHashMap<>();
    private volatile long lastRebuild;

    /** Обновляет карту известных команд сервера. */
    public void rebuild() {
        CommandMap commandMap = Bukkit.getCommandMap();
        for (Map.Entry<String, Command> entry : commandMap.getKnownCommands().entrySet()) {
            Command command = entry.getValue();
            if (command == null) {
                continue;
            }
            String canonical = command.getName().toLowerCase(Locale.ROOT);
            String label = entry.getKey().toLowerCase(Locale.ROOT);
            this.aliasToCommand.putIfAbsent(label, canonical);
        }
        this.lastRebuild = System.currentTimeMillis();
    }

    private String canonical(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        String canonical = this.aliasToCommand.get(lower);
        if (canonical == null && System.currentTimeMillis() - this.lastRebuild > REBUILD_INTERVAL_MS) {
            rebuild();
            canonical = this.aliasToCommand.get(lower);
        }
        return canonical != null ? canonical : lower;
    }

    /**
     * Проверяет, относится ли ключ из конфига (команда или её алиас)
     * к введённой игроком команде.
     */
    public boolean matches(String configCommand, String typedLabel) {
        String key = configCommand.toLowerCase(Locale.ROOT);
        String typed = typedLabel.toLowerCase(Locale.ROOT);
        return key.equals(typed) || canonical(typed).equals(canonical(key));
    }
}