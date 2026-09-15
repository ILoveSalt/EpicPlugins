package hgds.epicgrief.api.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public final class Cooldown {
    private Cooldown() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private static final Map<String, Map<String, Long>> PLAYERS = new ConcurrentHashMap<>();

    public static void addCooldown(String playerName, long ticks) {
        addCooldown(playerName, "global", ticks);
    }

    public static void addCooldown(Player gamer, long ticks) {
        addCooldown(gamer.getName(), ticks);
    }

    public static boolean hasCooldown(String playerName) {
        return hasCooldown(playerName, "global");
    }

    public static boolean hasCooldown(Player gamer) {
        return hasCooldown(gamer.getName(), "global");
    }

    public static boolean hasCooldown(Player gamer, String type) {
        return hasCooldown(gamer.getName(), type);
    }

    public static boolean hasCooldown(String playerName, String type) {
        if (playerName == null || type == null)
            return false;
        String name = playerName.toLowerCase();
        Map<String, Long> cooldownData = PLAYERS.get(name);
        return cooldownData != null && cooldownData.containsKey(type.toLowerCase());
    }

    public static int getCooldownLeft(String playerName, String type) {
        if (!hasCooldown(playerName, type))
            return 0;
        String name = playerName.toLowerCase();
        Map<String, Long> cooldownData = PLAYERS.get(name);
        if (cooldownData == null)
            return 0;
        Long startTime = cooldownData.get(type.toLowerCase());
        if (startTime == null)
            return 0;
        int time = (int) ((startTime - System.currentTimeMillis()) / 50L / 20L);
        return (time == 0) ? 1 : time;
    }

    public static int getCooldownLeft(Player gamer, String type) {
        return getCooldownLeft(gamer.getName(), type);
    }

    public static void addCooldown(String playerName, String type, long ticks) {
        String name = playerName.toLowerCase();
        long time = System.currentTimeMillis() + ticks * 50L;
        Map<String, Long> cooldownData = PLAYERS.get(name);
        if (cooldownData == null) {
            cooldownData = new ConcurrentHashMap<>();
            cooldownData.put(type.toLowerCase(), time);
            PLAYERS.put(name, cooldownData);
            return;
        }
        if (cooldownData.containsKey(type.toLowerCase()))
            return;
        cooldownData.put(type.toLowerCase(), time);
    }

    public static void addCooldown(Player gamer, String type, long ticks) {
        addCooldown(gamer.getName(), type, ticks);
    }

    public static boolean hasOrAddCooldown(Player gamer, String type, long tick) {
        if (!hasCooldown(gamer, type)) {
            addCooldown(gamer, type, tick);
            return false;
        }
        return true;
    }

    static {
        // Каждые 50 мс вычищаем истёкшие кулдауны
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            PLAYERS.forEach((name, cooldownData) -> cooldownData.values().removeIf(time -> time < now));
        }, 0L, 50L, TimeUnit.MILLISECONDS);
    }
}
