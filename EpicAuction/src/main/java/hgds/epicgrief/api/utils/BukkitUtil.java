package hgds.epicgrief.api.utils;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import hgds.epicgrief.market.Market;

public final class BukkitUtil {
    private BukkitUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static void callEventAsync(Event event) {
        runTaskAsync(() -> callEvent(event));
    }

    public static void runTask(Runnable runnable) {
        Bukkit.getScheduler().runTask((Plugin)Market.getInstance(), runnable);
    }

    public static void runTaskAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)Market.getInstance(), runnable);
    }

    public static void runTaskLater(long delay, Runnable runnable) {
        Bukkit.getScheduler().runTaskLater((Plugin)Market.getInstance(), runnable, delay);
    }

    public static void runTaskLaterAsync(long delay, Runnable runnable) {
        Bukkit.getScheduler().runTaskLaterAsynchronously((Plugin)Market.getInstance(), runnable, delay);
    }
}
