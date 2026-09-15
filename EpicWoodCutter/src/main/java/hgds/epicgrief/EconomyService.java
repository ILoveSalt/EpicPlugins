package hgds.epicgrief;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

final class EconomyService {

    private final JavaPlugin plugin;
    private Object economy;

    EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    boolean hook() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                plugin.getLogger().severe("Vault найден, но провайдер экономики не зарегистрирован.");
                return false;
            }

            economy = registration.getProvider();
            plugin.getLogger().info("Экономика подключена: " + economy.getClass().getName());
            return true;
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().severe("Vault не найден. Установите Vault и плагин экономики.");
            return false;
        }
    }

    boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0.0) {
            return false;
        }

        try {
            Object result = callPlayerMethod("depositPlayer", player, amount);
            return transactionSucceeded(result);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось начислить деньги игроку " + player.getName(), exception);
            return false;
        }
    }

    private Object callPlayerMethod(String methodName, OfflinePlayer player, double amount)
            throws ReflectiveOperationException {
        Method offlineMethod = findMethod(methodName, OfflinePlayer.class, double.class);
        if (offlineMethod != null) {
            return offlineMethod.invoke(economy, player, amount);
        }

        Method stringMethod = findMethod(methodName, String.class, double.class);
        if (stringMethod != null) {
            return stringMethod.invoke(economy, player.getName(), amount);
        }

        throw new NoSuchMethodException(methodName);
    }

    private Method findMethod(String methodName, Class<?> firstType, Class<?> secondType) {
        try {
            return economy.getClass().getMethod(methodName, firstType, secondType);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private boolean transactionSucceeded(Object result) {
        if (result instanceof Boolean value) {
            return value;
        }

        if (result == null) {
            return true;
        }

        try {
            Method method = result.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(method.invoke(result));
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }
}
