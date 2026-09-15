package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

final class WoodConfigService {

    private final JavaPlugin plugin;
    private final GroupService groupService;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private double moneyMin = 10.0;
    private double moneyMax = 50.0;
    private double randomChance = 0.01;
    private double randomCoins = 4500.0;
    private final Map<String, String> messages = new HashMap<>();
    private String scoreboardTitle = "&e&lWOOD";
    private List<String> scoreboardLines = List.of();
    private final Map<String, WoodRegionSettings> regions = new LinkedHashMap<>();

    private Object chatProvider;
    private boolean chatChecked;

    WoodConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.groupService = new GroupService(plugin);
    }

    void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        moneyMin = config.getDouble("money.min", 10.0);
        moneyMax = config.getDouble("money.max", 50.0);
        randomChance = parseChance(config.getString("random.change", "1%"));
        randomCoins = config.getDouble("random.coins", 4500.0);

        messages.clear();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }

        scoreboardTitle = config.getString("scoreboard.title", "&e&lWOOD");
        scoreboardLines = new ArrayList<>(config.getStringList("scoreboard.values"));

        regions.clear();
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        if (regionsSection != null) {
            for (String regionId : regionsSection.getKeys(false)) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionId);
                if (regionSection == null) {
                    continue;
                }

                double earn = regionSection.getDouble("earn", 5.0);
                long cooldown = regionSection.getLong("cooldown", 10L);
                regions.put(regionId.toLowerCase(Locale.ROOT), new WoodRegionSettings(regionId, earn, cooldown));
            }
        }

        groupService.load(config.getConfigurationSection("groups"));
    }

    GroupService groups() {
        return groupService;
    }

    Map<String, WoodRegionSettings> regions() {
        return regions;
    }

    double randomSalaryAmount() {
        if (moneyMin >= moneyMax) {
            return moneyMin;
        }

        if (isIntegral(moneyMin) && isIntegral(moneyMax)) {
            return ThreadLocalRandom.current().nextLong((long) moneyMin, (long) moneyMax + 1L);
        }

        return ThreadLocalRandom.current().nextDouble(moneyMin, moneyMax);
    }

    boolean rollRandomBonus() {
        return ThreadLocalRandom.current().nextDouble() < randomChance;
    }

    double randomBonusCoins() {
        return randomCoins;
    }

    String scoreboardTitle() {
        return scoreboardTitle;
    }

    List<String> scoreboardLines() {
        return scoreboardLines;
    }

    String message(String key) {
        return messages.getOrDefault(key, "");
    }

    void send(Player player, String key, Map<String, String> placeholders) {
        sendRaw(player, message(key), placeholders);
    }

    void sendRaw(Player player, String rawMessage, Map<String, String> placeholders) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        String message = applyPlaceholders(rawMessage, placeholders);
        if (message.regionMatches(true, 0, "actionbar:", 0, 10)) {
            player.sendActionBar(color(message.substring(10).trim()));
            return;
        }

        player.sendMessage(color(message));
    }

    String formatLine(Player player, WoodPlayerData data, WoodGroupSettings group, String line) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", player.getName());
        placeholders.put("vault_rankprefix", vaultPrefix(player));
        placeholders.put("backpackCurrent", Integer.toString(data.getBackpack()));
        placeholders.put("backpackMax", Integer.toString(group.maxBackpack()));
        placeholders.put("booster", formatBooster(group.booster()));
        placeholders.put("salary", formatMoney(data.getSalary()));
        placeholders.put("earn", formatMoney(data.getSalary()));
        return applyPlaceholders(line, placeholders);
    }

    Component color(String message) {
        return legacy.deserialize(message.replace('§', '&'));
    }

    String formatMoney(double value) {
        double rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (Math.abs(rounded - Math.rint(rounded)) < 0.000001) {
            return Long.toString(Math.round(rounded));
        }
        return Double.toString(rounded);
    }

    private String formatBooster(double booster) {
        if (Math.abs(booster - Math.rint(booster)) < 0.000001) {
            return Long.toString(Math.round(booster));
        }
        return Double.toString(booster);
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private double parseChance(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0.0;
        }

        String normalized = rawValue.trim();
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        try {
            double parsed = Double.parseDouble(normalized.replace(',', '.'));
            return parsed > 1.0 ? parsed / 100.0 : parsed;
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Некорректное значение random.change: " + rawValue);
            return 0.0;
        }
    }

    private boolean isIntegral(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String vaultPrefix(Player player) {
        if (!ensureVaultChat()) {
            return "";
        }

        Object value = invokeChat("getPlayerPrefix", new Class<?>[]{Player.class}, player);
        if (value == null) {
            value = invokeChat(
                    "getPlayerPrefix",
                    new Class<?>[]{String.class, String.class},
                    player.getWorld().getName(),
                    player.getName()
            );
        }

        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean ensureVaultChat() {
        if (chatChecked) {
            return chatProvider != null;
        }

        chatChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }

        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) chatClass);
            if (registration == null) {
                return false;
            }

            chatProvider = registration.getProvider();
            return chatProvider != null;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Vault chat service недоступен", exception);
            return false;
        }
    }

    private Object invokeChat(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (chatProvider == null) {
            return null;
        }

        try {
            Method method = chatProvider.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(chatProvider, args);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
