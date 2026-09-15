package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class EpicMoney extends JavaPlugin implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final Map<String, String> ENTITY_ALIASES = createEntityAliases();
    private static final Map<String, String> SOUND_ALIASES = createSoundAliases();

    private final Map<String, RewardRule> rewardRules = new HashMap<>();
    private final Set<String> disabledWorlds = new HashSet<>();

    private FileConfiguration entitiesConfig;
    private Object economy;
    private Sound rewardSound;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("entities.yml", false);

        loadSettings();
        loadEntities();

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Loaded " + rewardRules.size() + " enabled reward rules.");
    }

    @Override
    public void onDisable() {
        rewardRules.clear();
        disabledWorlds.clear();
        economy = null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null || isWorldDisabled(entity.getWorld().getName())) {
            return;
        }

        if (entity instanceof Player victim) {
            handlePlayerKill(victim, killer);
            return;
        }

        handleMobKill(entity, killer);
    }

    private void handleMobKill(LivingEntity entity, Player killer) {
        RewardRule rule = findRewardRule(entity.getType().name());

        if (rule == null || rule.money().percentage()) {
            return;
        }

        double money = roundMoney(rule.money().nextAmount());
        if (money <= 0.0 || !depositMoney(killer, money)) {
            return;
        }

        runRewardActions(null, killer, rule.displayName(), money);
    }

    private void handlePlayerKill(Player victim, Player killer) {
        if (victim.getUniqueId().equals(killer.getUniqueId())) {
            return;
        }

        RewardRule rule = findRewardRule("PLAYER");
        if (rule == null) {
            return;
        }

        double victimBalance = getBalance(victim);
        double money = rule.money().percentage()
                ? victimBalance * rule.money().percent() / 100.0
                : rule.money().nextAmount();

        money = Math.min(victimBalance, roundMoney(money));
        if (money <= 0.0 || !withdrawMoney(victim, money)) {
            return;
        }

        if (!depositMoney(killer, money)) {
            depositMoney(victim, money);
            getLogger().warning("Could not deposit reward to " + killer.getName() + "; refunded " + victim.getName() + ".");
            return;
        }

        runRewardActions(victim, killer, victim.getName(), money);
    }

    private void loadSettings() {
        reloadConfig();
        disabledWorlds.clear();

        for (String world : getConfig().getStringList("disabled-worlds")) {
            if (world != null && !world.equalsIgnoreCase("none")) {
                disabledWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        rewardSound = parseSound(getConfig().getString("sound"));
    }

    private void loadEntities() {
        File entitiesFile = new File(getDataFolder(), "entities.yml");
        entitiesConfig = YamlConfiguration.loadConfiguration(entitiesFile);
        rewardRules.clear();

        for (String key : entitiesConfig.getKeys(false)) {
            ConfigurationSection section = entitiesConfig.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enable", true)) {
                continue;
            }

            String moneyValue = section.getString("money");
            if (moneyValue == null || moneyValue.isBlank()) {
                getLogger().warning("Skipped " + key + ": money value is empty.");
                continue;
            }

            try {
                RewardMoney money = RewardMoney.parse(moneyValue);
                String display = section.getString("display", key);
                rewardRules.put(key.toUpperCase(Locale.ROOT), new RewardRule(display, money));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Skipped " + key + ": " + exception.getMessage());
            }
        }
    }

    private boolean setupEconomy() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = getServer().getServicesManager().getRegistration(economyClass);

            if (registration == null) {
                getLogger().severe("Vault was found, but no economy provider is registered.");
                return false;
            }

            economy = registration.getProvider();
            getLogger().info("Hooked economy provider: " + economy.getClass().getName());
            return true;
        } catch (ClassNotFoundException exception) {
            getLogger().severe("Vault was not found. Install Vault and an economy plugin.");
            return false;
        }
    }

    private RewardRule findRewardRule(String entityType) {
        String key = entityType.toUpperCase(Locale.ROOT);
        RewardRule rule = rewardRules.get(key);

        if (rule != null) {
            return rule;
        }

        String alias = ENTITY_ALIASES.get(key);
        return alias == null ? null : rewardRules.get(alias);
    }

    private boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    private void runRewardActions(Player victim, Player killer, String entityName, double money) {
        String formattedMoney = formatMoney(money);

        for (String rawAction : getConfig().getStringList("money-add-action")) {
            Action action = Action.parse(rawAction);
            if (action == null) {
                continue;
            }

            String message = applyPlaceholders(action.message(), victim, killer, entityName, formattedMoney);

            switch (action.target()) {
                case "message-entity" -> {
                    if (victim != null) {
                        sendMessage(victim, message);
                        playRewardSound(victim);
                    }
                }
                case "message-killer" -> {
                    sendMessage(killer, message);
                    playRewardSound(killer);
                }
                case "actionbar-entity" -> {
                    if (victim != null) {
                        victim.sendActionBar(color(message));
                    }
                }
                case "actionbar-killer" -> killer.sendActionBar(color(message));
                default -> getLogger().warning("Unknown action type: " + action.target());
            }
        }
    }

    private String applyPlaceholders(String message, Player victim, Player killer, String entityName, String money) {
        String victimName = victim == null ? entityName : victim.getName();

        return message
                .replace("{killer}", killer.getName())
                .replace("{entity}", entityName)
                .replace("{victim}", victimName)
                .replace("{player}", victimName)
                .replace("{money}", money);
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(color(message));
    }

    private Component color(String message) {
        return LEGACY.deserialize(message);
    }

    private void playRewardSound(Player player) {
        if (rewardSound != null) {
            player.playSound(player.getLocation(), rewardSound, 1.0F, 1.0F);
        }
    }

    private Sound parseSound(String soundName) {
        if (soundName == null || soundName.isBlank() || soundName.equalsIgnoreCase("none")) {
            return null;
        }

        String key = soundName.trim().toUpperCase(Locale.ROOT);
        Sound sound = getSound(key);

        if (sound != null) {
            return sound;
        }

        String alias = SOUND_ALIASES.get(key);
        sound = alias == null ? null : getSound(alias);

        if (sound == null) {
            getLogger().warning("Sound '" + soundName + "' was not found and will be ignored.");
        }

        return sound;
    }

    private Sound getSound(String soundName) {
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private double getBalance(OfflinePlayer player) {
        try {
            Object result = callEconomyPlayerMethod("getBalance", player);
            if (result instanceof Number number) {
                return Math.max(0.0, number.doubleValue());
            }
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Could not read balance for " + player.getName() + ".", exception);
        }

        return 0.0;
    }

    private boolean depositMoney(OfflinePlayer player, double money) {
        return callEconomyTransaction("depositPlayer", player, money);
    }

    private boolean withdrawMoney(OfflinePlayer player, double money) {
        return callEconomyTransaction("withdrawPlayer", player, money);
    }

    private boolean callEconomyTransaction(String methodName, OfflinePlayer player, double money) {
        try {
            Object result = callEconomyPlayerMethod(methodName, player, money);
            return transactionSucceeded(result);
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Economy transaction failed for " + player.getName() + ".", exception);
            return false;
        }
    }

    private Object callEconomyPlayerMethod(String methodName, OfflinePlayer player, Object... additionalArguments)
            throws ReflectiveOperationException {
        Class<?>[] offlineTypes = createParameterTypes(OfflinePlayer.class, additionalArguments);
        Method offlineMethod = findEconomyMethod(methodName, offlineTypes);

        if (offlineMethod != null) {
            return offlineMethod.invoke(economy, createArguments(player, additionalArguments));
        }

        Method stringMethod = findEconomyMethod(methodName, createParameterTypes(String.class, additionalArguments));
        if (stringMethod != null) {
            return stringMethod.invoke(economy, createArguments(player.getName(), additionalArguments));
        }

        throw new NoSuchMethodException(methodName);
    }

    private Method findEconomyMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return economy.getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private Class<?>[] createParameterTypes(Class<?> firstType, Object[] additionalArguments) {
        Class<?>[] parameterTypes = new Class<?>[additionalArguments.length + 1];
        parameterTypes[0] = firstType;

        for (int index = 0; index < additionalArguments.length; index++) {
            Object argument = additionalArguments[index];
            parameterTypes[index + 1] = argument instanceof Double ? double.class : argument.getClass();
        }

        return parameterTypes;
    }

    private Object[] createArguments(Object firstArgument, Object[] additionalArguments) {
        Object[] arguments = new Object[additionalArguments.length + 1];
        arguments[0] = firstArgument;
        System.arraycopy(additionalArguments, 0, arguments, 1, additionalArguments.length);
        return arguments;
    }

    private boolean transactionSucceeded(Object transactionResult) {
        if (transactionResult instanceof Boolean result) {
            return result;
        }

        if (transactionResult == null) {
            return true;
        }

        try {
            Method method = transactionResult.getClass().getMethod("transactionSuccess");
            Object result = method.invoke(transactionResult);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }

    private double roundMoney(double money) {
        return BigDecimal.valueOf(money).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatMoney(double money) {
        double rounded = roundMoney(money);
        if (Math.abs(rounded - Math.rint(rounded)) < 0.000001) {
            return String.valueOf((long) Math.rint(rounded));
        }

        return MONEY_FORMAT.format(rounded);
    }

    private static Map<String, String> createEntityAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("MOOSHROOM", "MUSHROOM_COW");
        aliases.put("MUSHROOM_COW", "MOOSHROOM");
        aliases.put("ZOMBIFIED_PIGLIN", "PIG_ZOMBIE");
        aliases.put("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
        aliases.put("SNOW_GOLEM", "SNOWMAN");
        aliases.put("SNOWMAN", "SNOW_GOLEM");
        return aliases;
    }

    private static Map<String, String> createSoundAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("BLOCK_NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING");
        return aliases;
    }

    private record RewardRule(String displayName, RewardMoney money) {
    }

    private record RewardMoney(boolean percentage, double min, double max, boolean integralRange) {

        static RewardMoney parse(String rawValue) {
            String normalized = rawValue.trim().replace(",", ".");

            if (normalized.endsWith("%")) {
                double percent = parsePositiveDouble(normalized.substring(0, normalized.length() - 1), rawValue);
                return new RewardMoney(true, percent, percent, false);
            }

            String[] rangeParts = normalized.split("\\s*-\\s*", 2);
            if (rangeParts.length == 2) {
                double min = parsePositiveDouble(rangeParts[0], rawValue);
                double max = parsePositiveDouble(rangeParts[1], rawValue);
                boolean integralRange = isIntegral(rangeParts[0]) && isIntegral(rangeParts[1]);

                if (max < min) {
                    double swap = min;
                    min = max;
                    max = swap;
                }

                return new RewardMoney(false, min, max, integralRange);
            }

            double money = parsePositiveDouble(normalized, rawValue);
            return new RewardMoney(false, money, money, isIntegral(normalized));
        }

        double percent() {
            return min;
        }

        double nextAmount() {
            if (percentage || min == max) {
                return min;
            }

            if (integralRange) {
                return ThreadLocalRandom.current().nextLong((long) min, (long) max + 1L);
            }

            return ThreadLocalRandom.current().nextDouble(min, max);
        }

        private static double parsePositiveDouble(String value, String originalValue) {
            try {
                double parsed = Double.parseDouble(value.trim());
                if (parsed < 0.0) {
                    throw new IllegalArgumentException("money value cannot be negative: " + originalValue);
                }
                return parsed;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("money value is invalid: " + originalValue);
            }
        }

        private static boolean isIntegral(String value) {
            String trimmed = value.trim();
            return !trimmed.contains(".") && !trimmed.contains(",");
        }
    }

    private record Action(String target, String message) {

        static Action parse(String rawAction) {
            if (rawAction == null || !rawAction.startsWith("[")) {
                return null;
            }

            int closingBracketIndex = rawAction.indexOf(']');
            if (closingBracketIndex <= 1 || closingBracketIndex == rawAction.length() - 1) {
                return null;
            }

            String target = rawAction.substring(1, closingBracketIndex).toLowerCase(Locale.ROOT);
            String message = rawAction.substring(closingBracketIndex + 1).trim();
            return new Action(target, message);
        }
    }
}
