package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Плагин автоматической выдачи зарплаты донат-группам.
 * Зарплата выдаётся сама, без команды: раз в checkTime минут плагин проходит
 * по всем онлайн-игрокам, определяет донат-группу по разрешению
 * epicsalary.salary.<группа> (ключ секции salary в config.yml), проверяет
 * кулдаун и, если время вышло, выполняет команды группы. Дата последней выдачи
 * каждого игрока хранится в файле данных salary.yml.
 */
public final class EpicSalary extends JavaPlugin {

    private static final String DATA_FILE = "salary.yml";
    private static final String PERMISSION_PREFIX = "epicsalary.salary.";

    /** Группы из конфига в порядке их объявления. */
    private final LinkedHashMap<String, ConfigurationSection> groups = new LinkedHashMap<>();

    /** Дата последней выдачи зарплаты по UUID игрока (в миллисекундах epoch). */
    private final Map<UUID, Long> lastSalaries = new HashMap<>();

    private FileConfiguration config;
    private BukkitTask salaryTask;

    @Override
    public void onEnable() {
        // Гарантируем наличие папки данных и config.yml в ней.
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Не удалось создать папку данных: " + dataFolder.getAbsolutePath());
        }
        ensureConfigYml();

        this.config = getConfig();

        loadGroups();
        if (groups.isEmpty()) {
            getLogger().warning("В config.yml не найдено ни одной донат-группы (секция 'salary'). Плагин не будет выдавать зарплату.");
            return;
        }

        loadData();

        long checkTimeMinutes = Math.max(1L, config.getLong("checkTime", 5L));
        long periodTicks = checkTimeMinutes * 60L * 20L;
        long initialDelayTicks = Math.min(periodTicks, 60L * 20L);

        salaryTask = Bukkit.getScheduler().runTaskTimer(this, this::checkOnlinePlayers, initialDelayTicks, periodTicks);

        getLogger().info("EpicSalary включён. Проверка зарплаты каждые " + checkTimeMinutes + " мин. Групп: " + groups.size() + ".");
    }

    @Override
    public void onDisable() {
        if (salaryTask != null) {
            salaryTask.cancel();
            salaryTask = null;
        }
        saveData();
    }

    private void loadGroups() {
        groups.clear();
        ConfigurationSection salary = config.getConfigurationSection("salary");
        if (salary == null) {
            return;
        }
        Set<String> keys = salary.getKeys(false);
        for (String key : keys) {
            ConfigurationSection section = salary.getConfigurationSection(key);
            if (section != null) {
                groups.put(key, section);
            }
        }
    }

    /**
     * Запускается по таймеру: выдает зарплату всем онлайн-игрокам, у которых
     * кулдаун истек.
     */
    private void checkOnlinePlayers() {
        long now = System.currentTimeMillis();
        Collection<? extends Player> online = Bukkit.getServer().getOnlinePlayers();
        for (Player player : online) {
            paySalary(player, now);
        }
    }

    private void paySalary(Player player, long now) {
        if (player == null || !player.isOnline()) {
            return;
        }

        String group = findDonateGroup(player);
        if (group == null) {
            return; // у игрока нет донат-группы (нет права epicsalary.salary.<группа>)
        }

        ConfigurationSection section = groups.get(group);
        long cooldownMillis = Math.max(0L, section.getLong("cooldown")) * 1000L;

        UUID uuid = player.getUniqueId();
        Long last = lastSalaries.get(uuid);
        long lastSalary = last == null ? 0L : last;

        // Кулдаун ещё не истёк — пропускаем игрока.
        if (now - lastSalary < cooldownMillis) {
            return;
        }

        // Выполняем команды зарплаты группы.
        for (String command : section.getStringList("commands")) {
            executeCommand(command, player);
        }

        // Записываем новую дату выдачи.
        lastSalaries.put(uuid, now);
        saveData();

        String message = section.getString("message");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(colorize(message));
        }
    }

    /** Возвращает первую (по порядку из конфига) донат-группу, на которую у игрока есть право. */
    private String findDonateGroup(Player player) {
        for (String group : groups.keySet()) {
            if (player.hasPermission(PERMISSION_PREFIX + group)) {
                return group;
            }
        }
        return null;
    }

    private void executeCommand(String command, Player player) {
        if (command == null || command.isEmpty()) {
            return;
        }
        String resolved = command.replace("{player}", player.getName());
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        try {
            Bukkit.getServer().dispatchCommand(console, resolved);
        } catch (CommandException e) {
            getLogger().warning("Не удалось выполнить команду '" + resolved + "': " + e.getMessage());
        }
    }

    // Хранение даты выдачи зарплаты (salary.yml).

/** Если config.yml отсутствует в папке данных — копирует его из jar плагина. */
    private void ensureConfigYml() {
        File cfg = new File(getDataFolder(), "config.yml");
        if (cfg.exists()) {
            return;
        }
        try (InputStream in = EpicSalary.class.getClassLoader().getResourceAsStream("config.yml")) {
            if (in == null) {
                getLogger().warning("Не найден config.yml в ресурсах плагина.");
                return;
            }
            File parent = cfg.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (OutputStream out = new FileOutputStream(cfg)) {
                in.transferTo(out);
            }
            getLogger().info("Создан config.yml: " + cfg.getAbsolutePath());
        } catch (IOException e) {
            getLogger().warning("Не удалось создать config.yml: " + e.getMessage());
        }
    }

    private File dataFile() {
        return new File(getDataFolder(), DATA_FILE);
    }

    private void loadData() {
        File file = dataFile();
        if (!file.exists()) {
            saveData(); // создаём файл данных по умолчанию
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map)) {
                return;
            }
            Map<String, Object> root = (Map<String, Object>) loaded;

            Object playersObj = root.get("players");
            if (playersObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) playersObj).entrySet()) {
                    Object lastObj = (entry.getValue() instanceof Map)
                            ? ((Map<?, ?>) entry.getValue()).get("last") : null;
                    long last = toLong(lastObj);
                    if (last <= 0L) {
                        continue;
                    }
                    try {
                        lastSalaries.put(UUID.fromString(entry.getKey().toString()), last);
                    } catch (IllegalArgumentException ignored) {
                        // некорректный UUID в файле данных — пропускаем
                    }
                }
            }
        } catch (IOException | YAMLException e) {
            getLogger().warning("Не удалось загрузить " + DATA_FILE + ": " + e.getMessage());
        }
    }

    private void saveData() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);

        Map<String, Object> players = new LinkedHashMap<>();
        for (Map.Entry<UUID, Long> entry : lastSalaries.entrySet()) {
            Map<String, Object> playerData = new LinkedHashMap<>();
            playerData.put("last", entry.getValue());
            players.put(entry.getKey().toString(), playerData);
        }
        root.put("players", players);

        File file = dataFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            getLogger().warning("Не удалось создать папку данных: " + parent.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            new Yaml().dump(root, writer);
        } catch (IOException e) {
            getLogger().warning("Не удалось сохранить " + DATA_FILE + ": " + e.getMessage());
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static String colorize(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "\u00A7");
    }
}
