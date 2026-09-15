package hgds.epicgrief.api.utils;

import java.math.BigDecimal;
import java.util.UUID;
import net.milkbowl.vault2.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import hgds.epicgrief.market.Market;

/**
 * Работа с экономикой через VaultUnlocked (net.milkbowl.vault2.economy.Economy).
 * Если VaultUnlocked не установлен, но есть классический Vault — используется он.
 */
public final class EconomyUtil {
    private static Economy ECONOMY_UNLOCKED;

    private static net.milkbowl.vault.economy.Economy ECONOMY_VAULT;

    private EconomyUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static void checkEconomy() {
        if (ECONOMY_UNLOCKED == null && ECONOMY_VAULT == null)
            throw new UnsupportedOperationException("VaultUnlocked/Vault plugin not found");
    }

    private static UUID getUUID(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        return (online != null) ? online.getUniqueId() : Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    public static void deposit(Player player, int money) {
        deposit(player.getName(), money);
    }

    public static void withdraw(Player player, int money) {
        withdraw(player.getName(), money);
    }

    public static int getBalance(Player player) {
        return getBalance(player.getName());
    }

    private static final String PLUGIN_NAME = "EpicAuction";

    public static void deposit(String playerName, int money) {
        checkEconomy();
        if (ECONOMY_UNLOCKED != null) {
            ECONOMY_UNLOCKED.deposit(PLUGIN_NAME, getUUID(playerName), BigDecimal.valueOf(money));
        } else {
            ECONOMY_VAULT.depositPlayer(playerName, money);
        }
    }

    public static void withdraw(String playerName, int money) {
        checkEconomy();
        if (ECONOMY_UNLOCKED != null) {
            ECONOMY_UNLOCKED.withdraw(PLUGIN_NAME, getUUID(playerName), BigDecimal.valueOf(money));
        } else {
            ECONOMY_VAULT.withdrawPlayer(playerName, money);
        }
    }

    public static int getBalance(String playerName) {
        checkEconomy();
        if (ECONOMY_UNLOCKED != null)
            return ECONOMY_UNLOCKED.balance(PLUGIN_NAME, getUUID(playerName)).intValue();
        return (int) ECONOMY_VAULT.getBalance(playerName);
    }

    public static boolean hasMoney(String playerName, int money) {
        checkEconomy();
        if (ECONOMY_UNLOCKED != null)
            return ECONOMY_UNLOCKED.has(PLUGIN_NAME, getUUID(playerName), BigDecimal.valueOf(money));
        return ECONOMY_VAULT.has(playerName, money);
    }

    static {
        if (Market.getInstance().getServer().getPluginManager().getPlugin("VaultUnlocked") != null) {
            ECONOMY_UNLOCKED = Market.getInstance().getServer().getServicesManager()
                    .getRegistration(Economy.class).getProvider();
        } else if (Market.getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
            ECONOMY_VAULT = Market.getInstance().getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
        }
    }
}

