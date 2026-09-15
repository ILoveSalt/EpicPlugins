package hgds.epicgrief;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class EpicSub extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private static final String STORAGE_FILE_NAME = "storage.yml";
    private static final String ADMIN_PERMISSION = "epicsub.admin";
    private static final String GIVE_PERMISSION = "epicsub.give";
    private static final String TAKE_PERMISSION = "epicsub.take";
    private static final String CHECK_OTHERS_PERMISSION = "epicsub.check.others";
    private static final String RELOAD_PERMISSION = "epicsub.reload";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private File storageFile;
    private YamlConfiguration storage;
    private BukkitTask checkTask;
    private LuckPerms luckPerms;
    private me.neznamy.tab.api.event.EventHandler<PlayerLoadEvent> tabLoadHandler;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            ensureStorage();
            reloadStorage();
            registerCommand();
            getServer().getPluginManager().registerEvents(this, this);
            hookLuckPerms();
            hookTab();
            startExpirationTask();
            applyForOnlinePlayers();
            getLogger().info("EpicSub enabled with " + getConfiguredPermissions().size() + " configured permissions.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "EpicSub could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        unhookTab();
        for (UUID uuid : new ArrayList<>(attachments.keySet())) {
            removeRuntimePermissions(uuid);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetSubscriptionVisuals(player);
        }
        saveStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;
            case "info":
                sendInfo(sender);
                return true;
            case "status":
            case "check":
                handleStatus(sender, args);
                return true;
            case "give":
            case "add":
            case "extend":
                handleGive(sender, args);
                return true;
            case "take":
            case "remove":
            case "del":
                handleTake(sender, args);
                return true;
            case "reload":
                handleReload(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("info", "status", "help"));
            if (hasAccess(sender, GIVE_PERMISSION)) {
                suggestions.add("give");
            }
            if (hasAccess(sender, TAKE_PERMISSION)) {
                suggestions.add("take");
            }
            if (hasAccess(sender, RELOAD_PERMISSION)) {
                suggestions.add("reload");
            }
            return filterSuggestions(suggestions, args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (subCommand.equals("give") || subCommand.equals("add") || subCommand.equals("extend")
                || subCommand.equals("take") || subCommand.equals("remove") || subCommand.equals("del")
                || subCommand.equals("status") || subCommand.equals("check"))) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return filterSuggestions(players, args[1]);
        }

        if (args.length == 3 && (subCommand.equals("give") || subCommand.equals("add") || subCommand.equals("extend"))) {
            return filterSuggestions(Arrays.asList("30d", "14d", "7d", "3d", "24h"), args[2]);
        }

        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (expireIfNeeded(player.getUniqueId(), player.getName(), true)) {
                return;
            }
            if (isSubscribed(player.getUniqueId())) {
                applyRuntimePermissions(player);
                applySubscriptionIntegrations(player);
                notifySubscriptionState(player);
            } else {
                removeRuntimePermissions(player.getUniqueId());
                removeLuckPermsPrefix(player.getUniqueId());
                resetSubscriptionVisuals(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeRuntimePermissions(event.getPlayer().getUniqueId());
    }

    private void handleStatus(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            if (!hasAccess(sender, CHECK_OTHERS_PERMISSION)) {
                sendConfiguredMessage(sender, "no_permission", Map.of());
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sendUsage(sender, "/sub status <ник>");
            return;
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? argsName(args, 1, "unknown") : target.getName();
        if (expireIfNeeded(uuid, name, true)) {
            sendConfiguredMessage(sender, "no_sub", variables(name, 0L));
            return;
        }

        long expiresAt = getExpiresAt(uuid);
        if (expiresAt <= System.currentTimeMillis()) {
            sendConfiguredMessage(sender, "no_sub", variables(name, 0L));
            return;
        }

        Map<String, String> variables = variables(name, expiresAt);
        if (args.length >= 2) {
            sendConfiguredMessage(sender, "admin_status", variables);
        } else {
            sendConfiguredMessage(sender, "sub_time", variables);
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!hasAccess(sender, GIVE_PERMISSION)) {
            sendConfiguredMessage(sender, "no_permission", Map.of());
            return;
        }
        if (args.length < 2) {
            sendUsage(sender, "/sub give <ник> [время]");
            return;
        }

        String targetName = args[1];
        String durationInput = args.length >= 3 ? args[2] : getConfig().getString("settings.default-duration", "30d");
        long durationMillis = parseDurationMillis(durationInput);
        if (durationMillis <= 0L) {
            sendConfiguredMessage(sender, "invalid_time", Map.of("input", durationInput));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();
        String storedName = target.getName() == null ? targetName : target.getName();
        long now = System.currentTimeMillis();
        long currentExpiresAt = getExpiresAt(uuid);
        boolean renewal = currentExpiresAt > now;
        long baseTime = renewal ? currentExpiresAt : now;
        long expiresAt = safeAdd(baseTime, durationMillis);

        setSubscription(uuid, storedName, expiresAt);
        applyLuckPermsPrefix(uuid, storedName, expiresAt);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            applyRuntimePermissions(onlineTarget);
            updateTabPrefix(onlineTarget);
            sendConfiguredMessage(onlineTarget, renewal ? "sub_renew" : "sub_buy", variables(storedName, expiresAt, durationMillis));
        }

        executePermissionCommands(storedName, getConfiguredPermissions(), true);
        saveStorage();
        sendConfiguredMessage(sender, renewal ? "admin_renew" : "admin_give", variables(storedName, expiresAt, durationMillis));
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (!hasAccess(sender, TAKE_PERMISSION)) {
            sendConfiguredMessage(sender, "no_permission", Map.of());
            return;
        }
        if (args.length < 2) {
            sendUsage(sender, "/sub take <ник>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? args[1] : target.getName();
        boolean hadSubscription = getExpiresAt(uuid) > System.currentTimeMillis();

        clearSubscription(uuid);
        revokeSubscription(uuid, name);
        saveStorage();

        if (hadSubscription) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                sendConfiguredMessage(onlineTarget, "sub_time_end", variables(name, 0L));
            }
        }
        sendConfiguredMessage(sender, "admin_take", variables(name, 0L));
    }

    private void handleReload(CommandSender sender) {
        if (!hasAccess(sender, RELOAD_PERMISSION)) {
            sendConfiguredMessage(sender, "no_permission", Map.of());
            return;
        }

        saveStorage();
        reloadConfig();
        reloadStorage();
        hookLuckPerms();
        hookTab();
        startExpirationTask();
        checkExpirations(false);
        applyForOnlinePlayers();
        sendConfiguredMessage(sender, "reload", Map.of());
    }

    private void registerCommand() {
        PluginCommand subCommand = getCommand("sub");
        if (subCommand == null) {
            getLogger().warning("Command 'sub' is missing from plugin.yml");
            return;
        }
        subCommand.setExecutor(this);
        subCommand.setTabCompleter(this);
    }

    private void ensureStorage() throws IOException {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder");
        }

        storageFile = new File(getDataFolder(), STORAGE_FILE_NAME);
        if (!storageFile.isFile()) {
            saveResource(STORAGE_FILE_NAME, false);
        }
    }

    private void reloadStorage() {
        storageFile = new File(getDataFolder(), STORAGE_FILE_NAME);
        storage = YamlConfiguration.loadConfiguration(storageFile);
        if (!storage.isConfigurationSection("players")) {
            storage.set("players", new LinkedHashMap<String, Object>());
            saveStorage();
        }
    }

    private void saveStorage() {
        if (storage == null || storageFile == null) {
            return;
        }

        try {
            storage.save(storageFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save storage.yml: " + exception.getMessage());
        }
    }

    private void startExpirationTask() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        long intervalSeconds = Math.max(10L, getConfig().getLong("settings.check-interval-seconds", 60L));
        checkTask = Bukkit.getScheduler().runTaskTimer(this, () -> checkExpirations(true), 20L, intervalSeconds * 20L);
    }

    private void checkExpirations(boolean notifyPlayers) {
        if (storage == null || storage.getConfigurationSection("players") == null) {
            return;
        }

        boolean changed = false;
        List<String> playerIds = new ArrayList<>(storage.getConfigurationSection("players").getKeys(false));
        for (String rawUuid : playerIds) {
            UUID uuid = parseUuid(rawUuid);
            if (uuid == null) {
                continue;
            }
            String name = storage.getString("players." + rawUuid + ".name", rawUuid);
            if (expireIfNeeded(uuid, name, notifyPlayers)) {
                changed = true;
                continue;
            }
            if (warnIfNeeded(uuid, name)) {
                changed = true;
            }
        }

        if (changed) {
            saveStorage();
        }
    }

    private boolean expireIfNeeded(UUID uuid, String playerName, boolean notifyPlayer) {
        long expiresAt = getExpiresAt(uuid);
        if (expiresAt <= 0L || expiresAt > System.currentTimeMillis()) {
            return false;
        }

        clearSubscription(uuid);
        revokeSubscription(uuid, playerName);
        Player player = Bukkit.getPlayer(uuid);
        if (notifyPlayer && player != null && player.isOnline()) {
            sendConfiguredMessage(player, "sub_time_end", variables(player.getName(), 0L));
        }
        saveStorage();
        return true;
    }

    private boolean warnIfNeeded(UUID uuid, String playerName) {
        long expiresAt = getExpiresAt(uuid);
        if (expiresAt <= 0L || expiresAt <= System.currentTimeMillis()) {
            return false;
        }
        if (storage.getBoolean("players." + uuid + ".warned", false)) {
            return false;
        }

        long warningBefore = parseDurationMillis(getConfig().getString("settings.warning-before", "3d"));
        if (warningBefore <= 0L || expiresAt - System.currentTimeMillis() > warningBefore) {
            return false;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        sendConfiguredMessage(player, "sub_time_end_soon", variables(playerName, expiresAt));
        storage.set("players." + uuid + ".warned", true);
        return true;
    }

    private void applyForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (expireIfNeeded(player.getUniqueId(), player.getName(), true)) {
                continue;
            }
            if (isSubscribed(player.getUniqueId())) {
                applyRuntimePermissions(player);
                applySubscriptionIntegrations(player);
            } else {
                removeRuntimePermissions(player.getUniqueId());
                removeLuckPermsPrefix(player.getUniqueId());
                resetSubscriptionVisuals(player);
            }
        }
    }

    private void notifySubscriptionState(Player player) {
        long expiresAt = getExpiresAt(player.getUniqueId());
        if (expiresAt <= System.currentTimeMillis()) {
            sendConfiguredMessage(player, "no_sub", variables(player.getName(), 0L));
            return;
        }

        long warningBefore = parseDurationMillis(getConfig().getString("settings.warning-before", "3d"));
        if (warningBefore > 0L && expiresAt - System.currentTimeMillis() <= warningBefore) {
            sendConfiguredMessage(player, "sub_time_end_soon", variables(player.getName(), expiresAt));
            storage.set("players." + player.getUniqueId() + ".warned", true);
            saveStorage();
            return;
        }
        sendConfiguredMessage(player, "sub_time", variables(player.getName(), expiresAt));
    }

    private void setSubscription(UUID uuid, String playerName, long expiresAt) {
        String path = "players." + uuid;
        storage.set(path + ".name", playerName);
        storage.set(path + ".expires-at", expiresAt);
        storage.set(path + ".warned", false);
        storage.set(path + ".updated-at", Instant.now().toString());
    }

    private void clearSubscription(UUID uuid) {
        storage.set("players." + uuid, null);
    }

    private boolean isSubscribed(UUID uuid) {
        return getExpiresAt(uuid) > System.currentTimeMillis();
    }

    private long getExpiresAt(UUID uuid) {
        if (storage == null) {
            return 0L;
        }
        return storage.getLong("players." + uuid + ".expires-at", 0L);
    }

    private void revokeSubscription(UUID uuid, String playerName) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            removeRuntimePermissions(uuid);
            resetSubscriptionVisuals(player);
        } else {
            removeRuntimePermissions(uuid);
        }
        removeLuckPermsPrefix(uuid);
        executePermissionCommands(playerName, getConfiguredPermissions(), false);
    }

    private void applyRuntimePermissions(Player player) {
        removeRuntimePermissions(player.getUniqueId());
        List<String> permissions = getConfiguredPermissions();
        if (permissions.isEmpty()) {
            return;
        }

        PermissionAttachment attachment = player.addAttachment(this);
        for (String permission : permissions) {
            attachment.setPermission(permission, true);
        }
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    private void removeRuntimePermissions(UUID uuid) {
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment == null) {
            return;
        }

        try {
            attachment.remove();
        } catch (IllegalArgumentException ignored) {
            // Bukkit throws if the attachment was already removed by player disconnect.
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.recalculatePermissions();
        }
    }

    private void applySubscriptionIntegrations(Player player) {
        applyLuckPermsPrefix(player.getUniqueId(), player.getName(), getExpiresAt(player.getUniqueId()));
        updateTabPrefix(player);
    }

    private void updateTabPrefix(Player player) {
        if (updateTabPluginPrefix(player)) {
            return;
        }

        String template = getFallbackTabTemplate();
        if (!template.isBlank()) {
            player.setPlayerListName(color(replaceVariables(template, variables(player.getName(), getExpiresAt(player.getUniqueId())))));
        }
    }

    private void resetSubscriptionVisuals(Player player) {
        resetTabPluginPrefix(player);
        player.setPlayerListName(null);
    }

    private String getFallbackTabTemplate() {
        String direct = getConfig().getString("subscription.fallback-tab-name", "");
        if (direct != null && !direct.isBlank()) {
            return direct;
        }

        String legacy = getConfig().getString("subscription.tab-player", "");
        if (legacy != null && !legacy.isBlank()) {
            return legacy;
        }

        List<String> legacyList = getConfig().getStringList("messages.sub_tab_player");
        if (!legacyList.isEmpty()) {
            return legacyList.get(0);
        }
        return "";
    }

    private void hookTab() {
        unhookTab();
        if (!isTabIntegrationEnabled() || !Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return;
        }

        try {
            TabAPI tabApi = TabAPI.getInstance();
            if (tabApi == null) {
                return;
            }
            tabLoadHandler = event -> {
                TabPlayer tabPlayer = event.getPlayer();
                if (tabPlayer == null) {
                    return;
                }
                UUID playerId = tabPlayer.getUniqueId();
                Bukkit.getScheduler().runTask(this, () -> {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        return;
                    }
                    if (isSubscribed(playerId)) {
                        updateTabPrefix(player);
                    } else {
                        resetTabPluginPrefix(player);
                    }
                });
            };
            tabApi.getEventBus().register(PlayerLoadEvent.class, tabLoadHandler);
        } catch (NoClassDefFoundError | RuntimeException exception) {
            getLogger().log(Level.WARNING, "Could not hook into TAB API", exception);
            tabLoadHandler = null;
        }
    }

    private void unhookTab() {
        if (tabLoadHandler == null) {
            return;
        }

        try {
            TabAPI tabApi = TabAPI.getInstance();
            if (tabApi != null) {
                tabApi.getEventBus().unregister(tabLoadHandler);
            }
        } catch (NoClassDefFoundError | RuntimeException ignored) {
        } finally {
            tabLoadHandler = null;
        }
    }

    private boolean updateTabPluginPrefix(Player player) {
        if (!isTabIntegrationEnabled() || !Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return false;
        }

        try {
            TabAPI tabApi = TabAPI.getInstance();
            if (tabApi == null) {
                return false;
            }

            TabPlayer tabPlayer = tabApi.getPlayer(player.getUniqueId());
            if (tabPlayer == null || !tabPlayer.isLoaded()) {
                return false;
            }

            String plusPrefix = color(getPlusPrefix());
            if (getConfig().getBoolean("subscription.tab.tablist", true)) {
                TabListFormatManager tabList = tabApi.getTabListFormatManager();
                if (tabList != null) {
                    tabList.setPrefix(tabPlayer, plusPrefix + stripKnownPlusPrefix(nullToEmpty(tabList.getOriginalReplacedPrefix(tabPlayer))));
                }
            }
            if (getConfig().getBoolean("subscription.tab.nametag", true)) {
                NameTagManager nameTag = tabApi.getNameTagManager();
                if (nameTag != null) {
                    nameTag.setPrefix(tabPlayer, plusPrefix + stripKnownPlusPrefix(nullToEmpty(nameTag.getOriginalReplacedPrefix(tabPlayer))));
                }
            }
            return true;
        } catch (NoClassDefFoundError | RuntimeException exception) {
            getLogger().log(Level.WARNING, "Could not update TAB prefix for " + player.getName(), exception);
            return false;
        }
    }

    private void resetTabPluginPrefix(Player player) {
        if (!isTabIntegrationEnabled() || !Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return;
        }

        try {
            TabAPI tabApi = TabAPI.getInstance();
            if (tabApi == null) {
                return;
            }

            TabPlayer tabPlayer = tabApi.getPlayer(player.getUniqueId());
            if (tabPlayer == null || !tabPlayer.isLoaded()) {
                return;
            }

            TabListFormatManager tabList = tabApi.getTabListFormatManager();
            if (tabList != null) {
                tabList.setPrefix(tabPlayer, null);
            }
            NameTagManager nameTag = tabApi.getNameTagManager();
            if (nameTag != null) {
                nameTag.setPrefix(tabPlayer, null);
            }
        } catch (NoClassDefFoundError | RuntimeException exception) {
            getLogger().log(Level.WARNING, "Could not reset TAB prefix for " + player.getName(), exception);
        }
    }

    private boolean isTabIntegrationEnabled() {
        return getConfig().getBoolean("subscription.tab.enabled", true);
    }

    private String getPlusPrefix() {
        return getConfig().getString("subscription.plus-prefix", "&6&l+");
    }

    private void hookLuckPerms() {
        if (!isLuckPermsPrefixEnabled() || !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            luckPerms = null;
            return;
        }

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (NoClassDefFoundError | IllegalStateException exception) {
            luckPerms = null;
            getLogger().log(Level.WARNING, "Could not hook into LuckPerms API", exception);
        }
    }

    private boolean ensureLuckPerms() {
        if (luckPerms != null) {
            return true;
        }
        hookLuckPerms();
        return luckPerms != null;
    }

    private boolean isLuckPermsPrefixEnabled() {
        return getConfig().getBoolean("subscription.luckperms.enabled", true);
    }

    private int getLuckPermsPrefixPriority() {
        return getConfig().getInt("subscription.luckperms.prefix-priority", 10000);
    }

    private void applyLuckPermsPrefix(UUID uuid, String playerName, long expiresAt) {
        if (uuid == null || expiresAt <= System.currentTimeMillis() || !isLuckPermsPrefixEnabled() || !ensureLuckPerms()) {
            return;
        }

        LuckPerms api = luckPerms;
        int priority = getLuckPermsPrefixPriority();
        String plusPrefix = getPlusPrefix();
        String coloredPlusPrefix = color(plusPrefix);
        String plainPlusPrefix = nullToEmpty(ChatColor.stripColor(coloredPlusPrefix));
        api.getUserManager().modifyUser(uuid, user -> {
            clearLuckPermsPrefixNodes(user, priority, plusPrefix, coloredPlusPrefix, plainPlusPrefix);
            String donatePrefix = stripKnownPlusPrefix(
                    nullToEmpty(user.getCachedData().getMetaData().getPrefix()),
                    plusPrefix,
                    coloredPlusPrefix,
                    plainPlusPrefix
            );
            PrefixNode node = PrefixNode.builder(plusPrefix + donatePrefix, priority)
                    .expiry(Instant.ofEpochMilli(expiresAt))
                    .build();
            user.data().add(node);
        }).exceptionally(exception -> {
            getLogger().log(Level.WARNING, "Could not update LuckPerms prefix for " + playerName, exception);
            return null;
        });
    }

    private void removeLuckPermsPrefix(UUID uuid) {
        if (uuid == null || !isLuckPermsPrefixEnabled() || !ensureLuckPerms()) {
            return;
        }

        LuckPerms api = luckPerms;
        int priority = getLuckPermsPrefixPriority();
        String plusPrefix = getPlusPrefix();
        String coloredPlusPrefix = color(plusPrefix);
        String plainPlusPrefix = nullToEmpty(ChatColor.stripColor(coloredPlusPrefix));
        api.getUserManager().modifyUser(uuid, user -> clearLuckPermsPrefixNodes(user, priority, plusPrefix, coloredPlusPrefix, plainPlusPrefix))
                .exceptionally(exception -> {
                    getLogger().log(Level.WARNING, "Could not remove LuckPerms prefix for " + uuid, exception);
                    return null;
                });
    }

    private void clearLuckPermsPrefixNodes(User user, int priority, String rawPrefix, String coloredPrefix, String plainPrefix) {
        user.data().clear(node -> isEpicSubLuckPermsPrefix(node, priority, rawPrefix, coloredPrefix, plainPrefix));
    }

    private boolean isEpicSubLuckPermsPrefix(Node node, int priority, String rawPrefix, String coloredPrefix, String plainPrefix) {
        if (!NodeType.PREFIX.matches(node)) {
            return false;
        }
        PrefixNode prefixNode = NodeType.PREFIX.cast(node);
        String metaValue = prefixNode.getMetaValue();
        return prefixNode.getPriority() == priority
                && !stripKnownPlusPrefix(metaValue, rawPrefix, coloredPrefix, plainPrefix).equals(nullToEmpty(metaValue));
    }

    private String stripKnownPlusPrefix(String value) {
        String text = nullToEmpty(value);
        String rawPrefix = getPlusPrefix();
        String coloredPrefix = color(rawPrefix);
        String plainPrefix = nullToEmpty(ChatColor.stripColor(coloredPrefix));
        return stripKnownPlusPrefix(text, rawPrefix, coloredPrefix, plainPrefix);
    }

    private String stripKnownPlusPrefix(String value, String rawPrefix, String coloredPrefix, String plainPrefix) {
        String text = nullToEmpty(value);
        if (!rawPrefix.isBlank() && text.startsWith(rawPrefix)) {
            return text.substring(rawPrefix.length());
        }
        if (!coloredPrefix.isBlank() && text.startsWith(coloredPrefix)) {
            return text.substring(coloredPrefix.length());
        }
        if (!plainPrefix.isBlank() && text.startsWith(plainPrefix)) {
            return text.substring(plainPrefix.length());
        }
        return text;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> getConfiguredPermissions() {
        List<String> permissions = new ArrayList<>(getConfig().getStringList("subscription.permissions"));
        if (permissions.isEmpty()) {
            permissions.addAll(getConfig().getStringList("messages.sub_buy_permision"));
        }

        List<String> normalized = new ArrayList<>();
        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) {
                continue;
            }
            normalized.add(permission.trim());
        }
        return normalized;
    }

    private void executePermissionCommands(String playerName, List<String> permissions, boolean add) {
        if (playerName == null || playerName.isBlank() || permissions.isEmpty()) {
            return;
        }

        String path = add ? "settings.permission-add-commands" : "settings.permission-remove-commands";
        List<String> commands = getConfig().getStringList(path);
        if (commands.isEmpty()) {
            return;
        }

        for (String permission : permissions) {
            for (String template : commands) {
                String command = template
                        .replace("%player%", playerName)
                        .replace("%player_name%", playerName)
                        .replace("%permission%", permission);
                dispatchConsoleCommand(command);
            }
        }
    }

    private void dispatchConsoleCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String prepared = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
    }

    private void sendInfo(CommandSender sender) {
        List<String> lines = getConfig().getStringList("messages.sub_info");
        if (lines.isEmpty()) {
            send(sender, "%prefix% &f/sub status");
            return;
        }

        Map<String, String> variables = baseVariables();
        if (sender instanceof Player player) {
            variables.putAll(variables(player.getName(), getExpiresAt(player.getUniqueId())));
        }
        for (String line : lines) {
            send(sender, replaceVariables(line, variables));
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> lines = getConfig().getStringList("messages.help");
        if (lines.isEmpty()) {
            send(sender, "%prefix% &f/sub info");
            send(sender, "%prefix% &f/sub status");
            if (hasAccess(sender, GIVE_PERMISSION)) {
                send(sender, "%prefix% &f/sub give <ник> [время]");
            }
            return;
        }

        for (String line : lines) {
            send(sender, replaceVariables(line, baseVariables()));
        }
    }

    private void sendUsage(CommandSender sender, String usage) {
        sendConfiguredMessage(sender, "usage", Map.of("usage", usage));
    }

    private void sendConfiguredMessage(CommandSender sender, String key, Map<String, String> variables) {
        String message = getConfig().getString("messages." + key);
        if (message == null || message.isBlank()) {
            return;
        }
        send(sender, replaceVariables(message, variables));
    }

    private void send(CommandSender sender, String message) {
        String colored = color(replaceVariables(message, baseVariables()));
        if (sender instanceof Player) {
            sender.sendMessage(colored);
        } else {
            sender.sendMessage(ChatColor.stripColor(colored));
        }
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input.replace(ChatColor.COLOR_CHAR, '&'));
    }

    private Map<String, String> variables(String playerName, long expiresAt) {
        return variables(playerName, expiresAt, Math.max(0L, expiresAt - System.currentTimeMillis()));
    }

    private Map<String, String> variables(String playerName, long expiresAt, long durationMillis) {
        Map<String, String> variables = baseVariables();
        String name = playerName == null ? "" : playerName;
        variables.put("player", name);
        variables.put("player_name", name);
        variables.put("time", formatDuration(durationMillis));
        variables.put("expires", formatDate(expiresAt));
        variables.put("expires_at", formatDate(expiresAt));
        return variables;
    }

    private Map<String, String> baseVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("prefix", getConfig().getString("settings.prefix", "&6&lEPIC+"));
        variables.put("plus", getPlusPrefix());
        variables.put("plus_prefix", getPlusPrefix());
        return variables;
    }

    private String replaceVariables(String input, Map<String, String> variables) {
        String result = input == null ? "" : input;
        Map<String, String> merged = new HashMap<>(baseVariables());
        if (variables != null) {
            merged.putAll(variables);
        }
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value);
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    private String formatDate(long epochMillis) {
        if (epochMillis <= 0L) {
            return "-";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + " " + plural(days, "день", "дня", "дней"));
        }
        if (hours > 0) {
            parts.add(hours + " " + plural(hours, "час", "часа", "часов"));
        }
        if (minutes > 0 && parts.size() < 2) {
            parts.add(minutes + " " + plural(minutes, "минута", "минуты", "минут"));
        }
        if (parts.isEmpty()) {
            parts.add(seconds + " " + plural(seconds, "секунда", "секунды", "секунд"));
        }
        return String.join(" ", parts);
    }

    private String plural(long value, String one, String few, String many) {
        long lastTwo = value % 100L;
        long last = value % 10L;
        if (lastTwo >= 11L && lastTwo <= 14L) {
            return many;
        }
        if (last == 1L) {
            return one;
        }
        if (last >= 2L && last <= 4L) {
            return few;
        }
        return many;
    }

    private long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return 0L;
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        int splitIndex = 0;
        while (splitIndex < normalized.length()) {
            char current = normalized.charAt(splitIndex);
            if ((current >= '0' && current <= '9') || current == '.') {
                splitIndex++;
                continue;
            }
            break;
        }

        if (splitIndex == 0) {
            return 0L;
        }

        double amount;
        try {
            amount = Double.parseDouble(normalized.substring(0, splitIndex));
        } catch (NumberFormatException exception) {
            return 0L;
        }

        String unit = normalized.substring(splitIndex).trim();
        if (unit.isEmpty()) {
            unit = "d";
        }

        double multiplier;
        if (unit.startsWith("s") || unit.startsWith("сек")) {
            multiplier = 1000D;
        } else if (unit.startsWith("m") || unit.startsWith("мин")) {
            multiplier = 60_000D;
        } else if (unit.startsWith("h") || unit.startsWith("ч")) {
            multiplier = 3_600_000D;
        } else if (unit.startsWith("w") || unit.startsWith("нед")) {
            multiplier = 604_800_000D;
        } else if (unit.startsWith("mo") || unit.startsWith("мес")) {
            multiplier = 2_592_000_000D;
        } else {
            multiplier = 86_400_000D;
        }

        double result = amount * multiplier;
        if (result <= 0D || result > Long.MAX_VALUE) {
            return 0L;
        }
        return (long) result;
    }

    private long safeAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private boolean hasAccess(CommandSender sender, String permission) {
        return !(sender instanceof Player) || sender.hasPermission(permission) || sender.hasPermission(ADMIN_PERMISSION);
    }

    private String argsName(String[] args, int index, String fallback) {
        if (args.length <= index || args[index] == null || args[index].isBlank()) {
            return fallback;
        }
        return args[index];
    }

    private UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private List<String> filterSuggestions(List<String> values, String input) {
        String normalizedInput = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                result.add(value);
            }
        }
        return result;
    }
}
