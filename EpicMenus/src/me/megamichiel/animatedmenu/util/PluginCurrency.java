package me.megamichiel.animatedmenu.util;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public abstract class PluginCurrency<N extends Number & Comparable<N>> {

    public static final PluginCurrency<Double> VAULT = new PluginCurrency<Double>() {
        Economy economy;

        @Override
        boolean init() {
            try {
                RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
                return registration != null && (economy = registration.getProvider()) != null;
            } catch (NoClassDefFoundError ex) {
                return false;
            }
        }

        @Override
        public Double get(Player player) {
            return isAvailable() ? economy.getBalance(player) : 0D;
        }

        @Override
        public boolean has(Player player, Double amount) {
            return isAvailable() && economy.has(player, amount);
        }

        @Override
        public void give(Player player, Double amount) {
            if (isAvailable()) economy.depositPlayer(player, amount);
        }

        @Override
        public boolean take(Player player, Double amount) {
            return isAvailable() && economy.withdrawPlayer(player, amount).transactionSuccess();
        }
    };
    public static final PluginCurrency<Integer> PLAYER_POINTS = new PluginCurrency<Integer>() {
        PlayerPointsAPI api;

        @Override
        boolean init() {
            try {
                return (api = ((PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints")).getAPI()) != null;
            } catch (NoClassDefFoundError | ClassCastException ex) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return isAvailable() ? api.look(player.getUniqueId()) : 0;
        }

        @Override
        public void give(Player player, Integer amount) {
            if (isAvailable()) {
                api.give(player.getUniqueId(), amount);
            }
        }

        @Override
        public boolean take(Player player, Integer amount) {
            return isAvailable() && api.take(player.getUniqueId(), amount);
        }

        @Override
        public boolean has(Player player, Integer amount) {
            return isAvailable() && api.look(player.getUniqueId()) >= amount;
        }
    };
    public static final PluginCurrency<Integer> GEMS = new PluginCurrency<Integer>() {

        Object methods;
        Method getGems, addGems, takeGems;

        @Override
        boolean init() {
            try {
                Class<?> gemMethods = Class.forName("me.JohnCrafted.gemseconomy.economy.GemMethods");
                Constructor<?> constructor = gemMethods.getDeclaredConstructor();
                constructor.setAccessible(true);
                methods = constructor.newInstance();
                getGems = gemMethods.getMethod("getGems", UUID.class);
                addGems = gemMethods.getMethod("addGems", UUID.class, int.class);
                takeGems = gemMethods.getMethod("takeGems", UUID.class, int.class);
                return true;
            } catch (Exception err) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return isAvailable() ? ((Number) invoke(methods, getGems, player.getUniqueId())).intValue() : 0;
        }

        @Override
        public void give(Player player, Integer amount) {
            if (isAvailable()) {
                invoke(methods, addGems, player.getUniqueId(), amount);
            }
        }

        @Override
        public boolean take(Player player, Integer amount) {
            if (isAvailable() && get(player) >= amount) {
                invoke(methods, takeGems, player.getUniqueId(), amount);
                return true;
            }
            return false;
        }

        @Override
        public boolean has(Player player, Integer amount) {
            return get(player) >= amount;
        }
    };
    public static final PluginCurrency<Integer> TOKENS = new PluginCurrency<Integer>() {

        Class<?> tmapi;
        Method getTokens, addTokens, removeTokens;

        @Override
        boolean init() {
            try {
                tmapi = Class.forName("me.realized.tm.api.TMAPI");
                getTokens = findMethod(tmapi, "getTokens", 1);
                addTokens = findMethod(tmapi, "addTokens", 2);
                removeTokens = findMethod(tmapi, "removeTokens", 2);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return ((Number) invoke(null, getTokens, player)).intValue();
        }

        @Override
        public void give(Player player, Integer amount) {
            invoke(null, addTokens, player, castNumber(amount, addTokens.getParameterTypes()[1]));
        }

        @Override
        public boolean take(Player player, Integer amount) {
            if (get(player) >= amount) {
                invoke(null, removeTokens, player, castNumber(amount, removeTokens.getParameterTypes()[1]));
                return true;
            }
            return false;
        }
    };
    public static final PluginCurrency<Double> COINS = new PluginCurrency<Double>() {

        boolean epicCoins;

        Class<?> coinsApi;
        Method getCoins, addCoins, takeCoins;

        Method epicCoinSystem, epicPlayerManager, epicGetPlayer, epicGetCoins, epicHasCoins, epicAddCoins, epicRemoveCoins;

        @Override
        boolean init() {
            return initEpicCoins() || initLegacyCoins();
        }

        private boolean initEpicCoins() {
            if (!Bukkit.getPluginManager().isPluginEnabled("EpicCoins")) {
                return false;
            }
            try {
                Class<?> coinSystem = Class.forName("hgds.epicgrief.epiccoins.core.CoinSystem");
                Class<?> coinPlayerManager = Class.forName("hgds.epicgrief.epiccoins.core.player.CoinPlayerManager");
                Class<?> coinPlayer = Class.forName("hgds.epicgrief.epiccoins.core.player.CoinPlayer");
                epicCoinSystem = findMethod(coinSystem, "getInstance");
                epicPlayerManager = findMethod(coinSystem, "getPlayerManager");
                epicGetPlayer = findMethod(coinPlayerManager, "getPlayer", UUID.class);
                epicGetCoins = findMethod(coinPlayer, "getCoins");
                epicHasCoins = findMethod(coinPlayer, "hasCoins", long.class);
                epicAddCoins = findMethod(coinPlayer, "addCoins", long.class);
                epicRemoveCoins = findMethod(coinPlayer, "removeCoins", long.class);
                return epicCoins = true;
            } catch (Exception ex) {
                return false;
            }
        }

        private boolean initLegacyCoins() {
            try {
                coinsApi = Class.forName("net.nifheim.beelzebu.coins.CoinsAPI");
                getCoins = findMethod(coinsApi, "getCoins", 1);
                addCoins = findMethod(coinsApi, "addCoins", 3);
                takeCoins = findMethod(coinsApi, "takeCoins", 2);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public Double get(Player player) {
            if (epicCoins) {
                Object coinPlayer = getEpicCoinPlayer(player);
                return coinPlayer == null ? 0D : ((Number) invoke(coinPlayer, epicGetCoins)).doubleValue();
            }
            return ((Number) invoke(null, getCoins, player.getUniqueId())).doubleValue();
        }

        @Override
        public boolean has(Player player, Double amount) {
            if (epicCoins) {
                Object coinPlayer = getEpicCoinPlayer(player);
                return coinPlayer != null && (Boolean) invoke(coinPlayer, epicHasCoins, amount.longValue());
            }
            return get(player) >= amount;
        }

        @Override
        public void give(Player player, Double amount) {
            if (epicCoins) {
                Object coinPlayer = getEpicCoinPlayer(player);
                if (coinPlayer != null) {
                    invoke(coinPlayer, epicAddCoins, amount.longValue());
                }
                return;
            }
            invoke(null, addCoins, player.getUniqueId(), castNumber(amount, addCoins.getParameterTypes()[1]), false);
        }

        @Override
        public boolean take(Player player, Double amount) {
            if (epicCoins) {
                Object coinPlayer = getEpicCoinPlayer(player);
                long coins = amount.longValue();
                if (coinPlayer != null && (Boolean) invoke(coinPlayer, epicHasCoins, coins)) {
                    invoke(coinPlayer, epicRemoveCoins, coins);
                    return true;
                }
                return false;
            }
            if (get(player) >= amount) {
                invoke(null, takeCoins, player.getUniqueId(), castNumber(amount, takeCoins.getParameterTypes()[1]));
                return true;
            }
            return false;
        }

        private Object getEpicCoinPlayer(Player player) {
            Object system = invoke(null, epicCoinSystem);
            if (system == null) {
                return null;
            }
            Object manager = invoke(system, epicPlayerManager);
            return manager == null ? null : invoke(manager, epicGetPlayer, player.getUniqueId());
        }
    };
    public static final PluginCurrency<Integer> POINTS_API = new PluginCurrency<Integer>() {

        Class<?> pointsApi;
        Method getPoints, addPoints, removePoints;

        @Override
        boolean init() {
            try {
                pointsApi = Class.forName("me.BukkitPVP.PointsAPI.PointsAPI");
                getPoints = findMethod(pointsApi, "getPoints", 1);
                addPoints = findMethod(pointsApi, "addPoints", 2);
                removePoints = findMethod(pointsApi, "removePoints", 2);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public Integer get(Player player) {
            return ((Number) invoke(null, getPoints, player)).intValue();
        }

        @Override
        public void give(Player player, Integer amount) {
            invoke(null, addPoints, player, castNumber(amount, addPoints.getParameterTypes()[1]));
        }

        @Override
        public boolean take(Player player, Integer amount) {
            if (get(player) >= amount) {
                invoke(null, removePoints, player, castNumber(amount, removePoints.getParameterTypes()[1]));
                return true;
            }
            return false;
        }
    };

    private boolean init, available;

    abstract boolean init();

    public final boolean isAvailable() {
        return init ? available : (init = true) & (available = init());
    }

    public abstract N get(Player player);
    public boolean has(Player player, N amount) {
        return get(player).compareTo(amount) >= 0;
    }
    public abstract void give(Player player, N amount);
    public abstract boolean take(Player player, N amount);

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = type.getMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Object castNumber(Number number, Class<?> type) {
        if (type == long.class || type == Long.class) return number.longValue();
        if (type == double.class || type == Double.class) return number.doubleValue();
        if (type == float.class || type == Float.class) return number.floatValue();
        if (type == short.class || type == Short.class) return number.shortValue();
        if (type == byte.class || type == Byte.class) return number.byteValue();
        return number.intValue();
    }
}
