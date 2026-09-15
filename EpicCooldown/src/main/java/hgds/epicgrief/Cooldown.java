package hgds.epicgrief;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import hgds.epicgrief.utils.Config;
import hgds.epicgrief.utils.Utils;

/**
 * Хранилище кулдаунов.
 * Ключ: "<ник>-<команда>", значение: время окончания (миллисекунды).
 * Потокобезопасно (автосохранение выполняется в асинхронной задаче).
 */
public final class Cooldown {

    private final JavaPlugin plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;

    public Cooldown(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Загружает кулдауны из cooldowns.yml (просроченные записи игнорируются). */
    public void load() {
        this.cooldowns.clear();
        FileConfiguration data = Config.getData("cooldowns.yml");
        ConfigurationSection section = data.getConfigurationSection("cooldowns");
        if (section == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : section.getKeys(false)) {
            long expireAt = section.getLong(key);
            if (expireAt > now) {
                this.cooldowns.put(key, expireAt);
            }
        }
    }

    /** Сохраняет кулдауны в cooldowns.yml (просроченные записи удаляются). */
    public void save() {
        FileConfiguration data = Config.getData("cooldowns.yml");
        data.set("cooldowns", null);
        this.cooldowns.forEach((key, expireAt) -> data.set("cooldowns." + key, expireAt));
        Config.save(data, "cooldowns.yml");
    }

    /** Запускает периодическое автосохранение (интервал — autosave-minutes из конфига). */
    public void startAutoSave() {
        stopAutoSave();
        long minutes = Utils.getConfig().getLong("autosave-minutes", 5L);
        if (minutes <= 0) {
            return;
        }
        long periodTicks = TimeUnit.MINUTES.toSeconds(minutes) * 20L;
        this.autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this.plugin, this::save, periodTicks, periodTicks);
    }

    public void stopAutoSave() {
        if (this.autoSaveTask != null) {
            this.autoSaveTask.cancel();
            this.autoSaveTask = null;
        }
    }

    /** Устанавливает кулдаун на указанное число секунд. */
    public void add(String key, int seconds) {
        this.cooldowns.put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    /** true, если кулдаун ещё активен. */
    public boolean has(String key) {
        Long expireAt = this.cooldowns.get(key);
        if (expireAt == null) {
            return false;
        }
        if (expireAt > System.currentTimeMillis()) {
            return true;
        }
        this.cooldowns.remove(key);
        return false;
    }

    /** Осталось секунд до окончания (с округлением вверх). */
    public int secondsLeft(String key) {
        Long expireAt = this.cooldowns.get(key);
        if (expireAt == null) {
            return 0;
        }
        long left = expireAt - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    /** Удаляет все кулдауны игрока. */
    public void clearPlayer(String playerName) {
        String prefix = playerName.toLowerCase(Locale.ROOT) + "-";
        this.cooldowns.keySet().removeIf(key -> key.toLowerCase(Locale.ROOT).startsWith(prefix));
    }

    /** Удаляет все кулдауны. */
    public void clearAll() {
        this.cooldowns.clear();
    }

    /** Количество активных записей. */
    public int size() {
        return this.cooldowns.size();
    }
}
