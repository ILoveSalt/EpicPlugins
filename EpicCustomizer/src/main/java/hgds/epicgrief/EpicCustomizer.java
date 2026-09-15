package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public final class EpicCustomizer extends JavaPlugin implements Listener, TabExecutor {
    private static final String COMMAND_PERMISSION = "epiccustomizer.use";
    private static final String ADMIN_PERMISSION = "epiccustomizer.admin";
    private static final String BYPASS_PERMISSION = "epiccustomizer.bypass";

    private File dataFile;
    private YamlConfiguration data;
    private Method placeholderApiMethod;
    private boolean placeholderApiChecked;
    private Class<?> vaultChatClass;
    private Object vaultChatProvider;
    private boolean vaultChatChecked;
    private Class<?> vaultPermissionClass;
    private Object vaultPermissionProvider;
    private boolean vaultPermissionChecked;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDataFile();
        registerCommand();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveData();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        String message = renderSelectedMessage(CustomizationType.JOIN, event.getPlayer(), null);
        if (!message.isBlank()) {
            event.setJoinMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        String message = renderSelectedMessage(CustomizationType.LEAVE, event.getPlayer(), null);
        if (!message.isBlank()) {
            event.setQuitMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        String message = renderSelectedMessage(CustomizationType.KILL, killer, victim);
        if (!message.isBlank()) {
            event.setDeathMessage(message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("customizer")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }

        if (!(sender instanceof Player player)) {
            send(sender, "messages.only-players", "&cOnly players can use this command.");
            return true;
        }

        if (!canUseCommand(sender)) {
            send(sender, "messages.no-perm", "&cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        CustomizationType type = CustomizationType.from(args[1]);
        if (type == null) {
            send(sender, "messages.type-null", "&cUnknown customization type.");
            return true;
        }

        return switch (action) {
            case "set" -> setCustomization(player, label, type, args);
            case "clear" -> clearCustomization(player, label, type, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("customizer")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> actions = new ArrayList<>(List.of("set", "clear"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                actions.add("reload");
            }
            return startsWith(actions, args[0]);
        }

        if (args.length == 2 && isSetOrClear(args[0])) {
            return startsWith(List.of("join", "kill", "leave"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            CustomizationType type = CustomizationType.from(args[1]);
            if (type == null) {
                return Collections.emptyList();
            }
            return startsWith(configuredIds(type), args[2]);
        }

        return Collections.emptyList();
    }

    private boolean setCustomization(Player player, String label, CustomizationType type, String[] args) {
        if (args.length != 3) {
            sendUsage(player, label);
            return true;
        }

        String configuredId = findConfiguredId(type, cleanId(args[2]));
        if (configuredId == null) {
            send(player, "messages.id-null", "&cThis customization was not found.");
            return true;
        }

        if (!canUseCustomization(player, type, configuredId)) {
            send(player, "messages.group-absent", "&cYou do not have this customization.");
            return true;
        }

        String selected = selectedId(player.getUniqueId(), type);
        if (configuredId.equalsIgnoreCase(selected)) {
            send(player, "messages.already-set", "&cThis customization is already selected.");
            return true;
        }

        setSelectedId(player, type, configuredId);
        if (!saveData()) {
            send(player, "messages.save-error", "&cCould not save customization.");
            return true;
        }

        sendTypeMessage(player, "messages.success-set.", type, "&aCustomization selected.");
        return true;
    }

    private boolean clearCustomization(Player player, String label, CustomizationType type, String[] args) {
        if (args.length != 2) {
            sendUsage(player, label);
            return true;
        }

        setSelectedId(player, type, null);
        if (!saveData()) {
            send(player, "messages.save-error", "&cCould not save customization.");
            return true;
        }

        sendTypeMessage(player, "messages.success-clear.", type, "&aCustomization cleared.");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            send(sender, "messages.no-perm", "&cNo permission.");
            return true;
        }

        reloadConfig();
        setupDataFile();
        refreshHooks();
        send(sender, "messages.reload-success", "&aEpicCustomizer reloaded.");
        return true;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("customizer");
        if (command == null) {
            getLogger().warning("Command /customizer is missing in plugin.yml.");
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void setupDataFile() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (getResource("data.yml") != null) {
                    saveResource("data.yml", false);
                } else if (!dataFile.createNewFile()) {
                    getLogger().warning("Could not create data.yml.");
                }
            } catch (IOException exception) {
                getLogger().log(Level.SEVERE, "Could not create data.yml.", exception);
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private boolean saveData() {
        if (data == null || dataFile == null) {
            return true;
        }

        try {
            data.save(dataFile);
            return true;
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save data.yml.", exception);
            return false;
        }
    }

    private String renderSelectedMessage(CustomizationType type, Player player, Player victim) {
        String selectedId = selectedId(player.getUniqueId(), type);
        if (selectedId.isBlank()) {
            return "";
        }

        String configuredId = findConfiguredId(type, selectedId);
        if (configuredId == null || !canUseCustomization(player, type, configuredId)) {
            return "";
        }

        String path = type.configKey + "." + configuredId + ".message";
        List<String> lines = getConfig().getStringList(path);
        if (lines.isEmpty()) {
            String line = getConfig().getString(path, "");
            if (!line.isBlank()) {
                lines = Collections.singletonList(line);
            }
        }

        List<String> rendered = new ArrayList<>();
        for (String line : lines) {
            String message = renderPlaceholders(line, player, victim);
            if (!message.isBlank()) {
                rendered.add(message);
            }
        }
        return String.join("\n", rendered);
    }

    private String renderPlaceholders(String text, Player player, Player victim) {
        String result = text == null ? "" : text;
        result = result
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%name%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%prefix%", vaultChatValue("getPlayerPrefix", player))
                .replace("%suffix%", vaultChatValue("getPlayerSuffix", player));

        if (victim != null) {
            result = result
                    .replace("%killer_prefix%", vaultChatValue("getPlayerPrefix", player))
                    .replace("%killer_suffix%", vaultChatValue("getPlayerSuffix", player))
                    .replace("%killer_name%", player.getName())
                    .replace("%killer%", player.getName())
                    .replace("%death_prefix%", vaultChatValue("getPlayerPrefix", victim))
                    .replace("%death_suffix%", vaultChatValue("getPlayerSuffix", victim))
                    .replace("%death_name%", victim.getName())
                    .replace("%death%", victim.getName())
                    .replace("%victim%", victim.getName())
                    .replace("%victim_name%", victim.getName());
        }

        result = applyPlaceholderApi(player, result);
        result = applyVaultPlaceholders(player, result);
        return color(result);
    }

    private boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission(COMMAND_PERMISSION) || sender.hasPermission(ADMIN_PERMISSION);
    }

    private boolean canUseCustomization(Player player, CustomizationType type, String id) {
        if (player.hasPermission(ADMIN_PERMISSION) || player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }

        List<String> groups = getConfig().getStringList(type.configKey + "." + id + ".groups");
        if (groups.isEmpty()) {
            return true;
        }

        for (String group : groups) {
            String normalized = group.toLowerCase(Locale.ROOT);
            if (player.hasPermission("group." + normalized)
                    || player.hasPermission("customizer.group." + normalized)
                    || player.hasPermission("epiccustomizer.group." + normalized)
                    || vaultPlayerInGroup(player, group)) {
                return true;
            }
        }
        return false;
    }

    private String selectedId(UUID uuid, CustomizationType type) {
        if (data == null) {
            return "";
        }

        String basePath = playerPath(uuid);
        String selected = data.getString(basePath + "." + type.dataKey, "");
        if (selected.isBlank() && type == CustomizationType.LEAVE) {
            selected = data.getString(basePath + ".quit", "");
        }
        return selected == null ? "" : selected.trim();
    }

    private void setSelectedId(Player player, CustomizationType type, String id) {
        if (data == null) {
            return;
        }

        String basePath = playerPath(player.getUniqueId());
        data.set(basePath + ".name", player.getName());
        data.set(basePath + "." + type.dataKey, id);
        if (type == CustomizationType.LEAVE) {
            data.set(basePath + ".quit", null);
        }
    }

    private String playerPath(UUID uuid) {
        return "players." + uuid;
    }

    private String findConfiguredId(CustomizationType type, String requestedId) {
        ConfigurationSection section = getConfig().getConfigurationSection(type.configKey);
        if (section == null || requestedId.isBlank()) {
            return null;
        }

        for (String id : section.getKeys(false)) {
            if (id.equalsIgnoreCase(requestedId)) {
                return id;
            }
        }
        return null;
    }

    private List<String> configuredIds(CustomizationType type) {
        ConfigurationSection section = getConfig().getConfigurationSection(type.configKey);
        if (section == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    private String cleanId(String raw) {
        String id = raw == null ? "" : raw.trim();
        if (id.length() > 2 && id.startsWith("%") && id.endsWith("%")) {
            id = id.substring(1, id.length() - 1);
        }
        return id;
    }

    private void sendUsage(CommandSender sender, String label) {
        String fallback = "&e/" + label + " set <join|kill|leave> <id>\n&e/" + label + " clear <join|kill|leave>";
        send(sender, "messages.usage", fallback);
    }

    private void sendTypeMessage(CommandSender sender, String prefix, CustomizationType type, String fallback) {
        String message = getConfig().getString(prefix + type.messageKey);
        if ((message == null || message.isBlank()) && type.legacyMessageKey != null) {
            message = getConfig().getString(prefix + type.legacyMessageKey);
        }
        sender.sendMessage(color(message == null || message.isBlank() ? fallback : message));
    }

    private void send(CommandSender sender, String path, String fallback) {
        String message = getConfig().getString(path, fallback);
        sender.sendMessage(color(message == null || message.isBlank() ? fallback : message));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    private List<String> startsWith(List<String> values, String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(value);
            }
        }
        return result;
    }

    private boolean isSetOrClear(String action) {
        return action.equalsIgnoreCase("set") || action.equalsIgnoreCase("clear");
    }

    private void refreshHooks() {
        placeholderApiMethod = null;
        placeholderApiChecked = false;
        vaultChatClass = null;
        vaultChatProvider = null;
        vaultChatChecked = false;
        vaultPermissionClass = null;
        vaultPermissionProvider = null;
        vaultPermissionChecked = false;
    }

    private String applyPlaceholderApi(Player player, String text) {
        if (!ensurePlaceholderApi()) {
            return text;
        }

        try {
            Object value = placeholderApiMethod.invoke(null, player, text);
            return value == null ? text : String.valueOf(value);
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "PlaceholderAPI hook failed", exception);
            placeholderApiMethod = null;
            placeholderApiChecked = true;
            return text;
        }
    }

    private boolean ensurePlaceholderApi() {
        if (placeholderApiChecked) {
            return placeholderApiMethod != null;
        }

        placeholderApiChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }

        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderApiMethod = placeholderApiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            return true;
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "PlaceholderAPI was found, but could not be hooked", exception);
            return false;
        }
    }

    private String applyVaultPlaceholders(Player player, String text) {
        if (text.indexOf("%vault_") < 0) {
            return text;
        }

        return text
                .replace("%vault_prefix%", vaultChatValue("getPlayerPrefix", player))
                .replace("%vault_suffix%", vaultChatValue("getPlayerSuffix", player));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean ensureVaultChat() {
        if (vaultChatChecked) {
            return vaultChatProvider != null;
        }

        vaultChatChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }

        try {
            vaultChatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) vaultChatClass);
            if (registration == null) {
                return false;
            }
            vaultChatProvider = registration.getProvider();
            return vaultChatProvider != null;
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Vault was found, but chat service could not be hooked", exception);
            return false;
        }
    }

    private String vaultChatValue(String methodName, Player player) {
        if (!ensureVaultChat()) {
            return "";
        }

        Object value = invokeVault(vaultChatClass, vaultChatProvider, methodName, new Class<?>[]{Player.class}, player);
        if (value == null) {
            value = invokeVault(
                    vaultChatClass,
                    vaultChatProvider,
                    methodName,
                    new Class<?>[]{String.class, String.class},
                    player.getWorld().getName(),
                    player.getName()
            );
        }
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean ensureVaultPermission() {
        if (vaultPermissionChecked) {
            return vaultPermissionProvider != null;
        }

        vaultPermissionChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }

        try {
            vaultPermissionClass = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) vaultPermissionClass);
            if (registration == null) {
                return false;
            }
            vaultPermissionProvider = registration.getProvider();
            return vaultPermissionProvider != null;
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Vault was found, but permission service could not be hooked", exception);
            return false;
        }
    }

    private boolean vaultPlayerInGroup(Player player, String group) {
        if (!ensureVaultPermission()) {
            return false;
        }

        Boolean direct = invokeVaultBoolean("playerInGroup", new Class<?>[]{Player.class, String.class}, player, group);
        if (direct != null) {
            return direct;
        }

        Boolean offline = invokeVaultBoolean(
                "playerInGroup",
                new Class<?>[]{String.class, OfflinePlayer.class, String.class},
                player.getWorld().getName(),
                player,
                group
        );
        if (offline != null) {
            return offline;
        }

        Boolean legacy = invokeVaultBoolean(
                "playerInGroup",
                new Class<?>[]{String.class, String.class, String.class},
                player.getWorld().getName(),
                player.getName(),
                group
        );
        if (legacy != null) {
            return legacy;
        }

        return containsGroup(group, invokeVault(
                vaultPermissionClass,
                vaultPermissionProvider,
                "getPlayerGroups",
                new Class<?>[]{Player.class},
                player
        )) || containsGroup(group, invokeVault(
                vaultPermissionClass,
                vaultPermissionProvider,
                "getPlayerGroups",
                new Class<?>[]{String.class, String.class},
                player.getWorld().getName(),
                player.getName()
        )) || equalsGroup(group, invokeVault(
                vaultPermissionClass,
                vaultPermissionProvider,
                "getPrimaryGroup",
                new Class<?>[]{Player.class},
                player
        )) || equalsGroup(group, invokeVault(
                vaultPermissionClass,
                vaultPermissionProvider,
                "getPrimaryGroup",
                new Class<?>[]{String.class, String.class},
                player.getWorld().getName(),
                player.getName()
        ));
    }

    private Boolean invokeVaultBoolean(String methodName, Class<?>[] parameterTypes, Object... args) {
        Object value = invokeVault(vaultPermissionClass, vaultPermissionProvider, methodName, parameterTypes, args);
        return value instanceof Boolean bool ? bool : null;
    }

    private Object invokeVault(Class<?> owner, Object provider, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (owner == null || provider == null) {
            return null;
        }

        try {
            Method method = owner.getMethod(methodName, parameterTypes);
            return method.invoke(provider, args);
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean containsGroup(String expected, Object rawGroups) {
        if (!(rawGroups instanceof String[] groups)) {
            return false;
        }

        for (String group : groups) {
            if (equalsGroup(expected, group)) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsGroup(String expected, Object actual) {
        return actual != null && expected.equalsIgnoreCase(String.valueOf(actual));
    }

    private enum CustomizationType {
        JOIN("join", "join", "join", null),
        KILL("kill", "kill", "kill", null),
        LEAVE("leave", "quit", "leave", "quit");

        private final String messageKey;
        private final String configKey;
        private final String dataKey;
        private final String legacyMessageKey;

        CustomizationType(String messageKey, String configKey, String dataKey, String legacyMessageKey) {
            this.messageKey = messageKey;
            this.configKey = configKey;
            this.dataKey = dataKey;
            this.legacyMessageKey = legacyMessageKey;
        }

        private static CustomizationType from(String raw) {
            if (raw == null) {
                return null;
            }

            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "join" -> JOIN;
                case "kill" -> KILL;
                case "leave", "quit" -> LEAVE;
                default -> null;
            };
        }
    }
}
