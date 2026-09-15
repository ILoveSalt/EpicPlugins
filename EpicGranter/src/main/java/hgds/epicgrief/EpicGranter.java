package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class EpicGranter extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final String CONFIG_FILE_NAME = "Config.yml";
    private static final String STORAGE_FILE_NAME = "storage.yml";
    private static final String ADMIN_PERMISSION = "epicgranter.admin";
    private static final String RELOAD_PERMISSION = "epicgranter.reload";

    private File configFile;
    private File storageFile;
    private YamlConfiguration config;
    private YamlConfiguration storage;

    @Override
    public void onEnable() {
        try {
            ensureResources();
            reloadFiles();
            registerCommands();
            getLogger().info("EpicGranter enabled with " + getConfiguredGroups().size() + " configured groups.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "EpicGranter could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        saveStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "give":
                handleGive(sender, args);
                return true;
            case "limits":
                handleLimits(sender);
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
            List<String> suggestions = new ArrayList<>();
            suggestions.add("give");
            suggestions.add("limits");
            if (sender.hasPermission(RELOAD_PERMISSION) || hasAdminAccess(sender)) {
                suggestions.add("reload");
            }
            return filterSuggestions(suggestions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return filterSuggestions(players, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filterSuggestions(getGrantableGroups(sender), args[2]);
        }

        return Collections.emptyList();
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendHelp(sender);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sendConfiguredMessage(sender, "playerNotFound", Map.of());
            return;
        }

        String targetGroup = normalizeGroup(args[2]);
        if (!isKnownGroup(targetGroup)) {
            sendConfiguredMessage(sender, "isntCanInConfig", Map.of());
            return;
        }

        boolean bypass = hasAdminAccess(sender);
        if (sender instanceof Player player) {
            if (player.getUniqueId().equals(target.getUniqueId())) {
                sendConfiguredMessage(sender, "notForMe", Map.of());
                return;
            }
            if (!bypass && !canPlayerGrant(player, target, targetGroup)) {
                return;
            }
        } else if (!bypass) {
            sendHelp(sender);
            return;
        }

        if (!dispatchGrantCommand(target.getName(), targetGroup)) {
            sendFallback(sender, "&6&lGRANT: &fCould not execute grant command.");
            return;
        }

        String senderName = sender.getName();
        String groupName = getGroupDisplayName(targetGroup);
        Map<String, String> variables = variables(senderName, target.getName(), targetGroup, groupName);
        if (sender instanceof Player player && !bypass) {
            recordGrant(player, target, targetGroup);
        }

        sendConfiguredMessage(sender, "successfulGiveMessage", variables);
        sendGrantTitle(variables);
        saveStorage();
    }

    private boolean canPlayerGrant(Player sender, Player target, String targetGroup) {
        String senderGroup = getHighestConfiguredGroup(sender);
        ConfigurationSection limits = config.getConfigurationSection("limits." + senderGroup);
        if (limits == null || limits.getKeys(false).isEmpty()) {
            sendConfiguredMessage(sender, "groupNotAllowed", Map.of());
            return false;
        }

        int maxLimit = limits.getInt(targetGroup, -1);
        if (maxLimit <= 0) {
            sendConfiguredMessage(sender, "isntCanInConfig", Map.of());
            return false;
        }

        int targetPriority = getGroupPriority(getHighestConfiguredGroup(target));
        int requestedPriority = getGroupPriority(targetGroup);
        if (requestedPriority >= 0 && targetPriority >= requestedPriority) {
            sendConfiguredMessage(sender, "priorityLow", Map.of());
            return false;
        }

        int used = getUsedLimit(sender.getUniqueId(), targetGroup);
        if (used >= maxLimit) {
            sendConfiguredMessage(sender, "limit", Map.of());
            return false;
        }

        return true;
    }

    private void handleLimits(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendFallback(sender, "&6&lGRANT: &fThis command is only available for players.");
            return;
        }

        String senderGroup = getHighestConfiguredGroup(player);
        ConfigurationSection limits = config.getConfigurationSection("limits." + senderGroup);
        if (limits == null || limits.getKeys(false).isEmpty()) {
            sendConfiguredMessage(sender, "groupNotAllowed", Map.of());
            return;
        }

        List<String> formattedLimits = new ArrayList<>();
        for (String rawGroup : limits.getKeys(false)) {
            String group = normalizeGroup(rawGroup);
            int max = limits.getInt(rawGroup);
            int used = getUsedLimit(player.getUniqueId(), group);
            formattedLimits.add(getGroupDisplayName(group) + ChatColor.WHITE + ": " + ChatColor.GOLD + used + "/" + max);
        }

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("limits", String.join(ChatColor.WHITE + ", ", formattedLimits));
        sendConfiguredMessage(sender, "limits", variables);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION) && !hasAdminAccess(sender)) {
            sendConfiguredMessage(sender, "groupNotAllowed", Map.of());
            return;
        }

        saveStorage();
        reloadFiles();
        sendFallback(sender, "&6&lGRANT: &fConfiguration reloaded.");
    }

    private void registerCommands() {
        PluginCommand grantCommand = getCommand("grant");
        if (grantCommand == null) {
            getLogger().warning("Command 'grant' is missing from plugin.yml");
            return;
        }
        grantCommand.setExecutor(this);
        grantCommand.setTabCompleter(this);
    }

    private void ensureResources() throws IOException {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder");
        }

        File uppercaseConfig = new File(getDataFolder(), CONFIG_FILE_NAME);
        File lowercaseConfig = new File(getDataFolder(), "config.yml");
        if (!uppercaseConfig.isFile() && !lowercaseConfig.isFile()) {
            saveResource(CONFIG_FILE_NAME, false);
        }

        storageFile = new File(getDataFolder(), STORAGE_FILE_NAME);
        if (!storageFile.isFile()) {
            saveResource(STORAGE_FILE_NAME, false);
        }
    }

    private void reloadFiles() {
        configFile = new File(getDataFolder(), CONFIG_FILE_NAME);
        File lowercaseConfig = new File(getDataFolder(), "config.yml");
        if (!configFile.isFile() && lowercaseConfig.isFile()) {
            configFile = lowercaseConfig;
        }

        storageFile = new File(getDataFolder(), STORAGE_FILE_NAME);
        config = YamlConfiguration.loadConfiguration(configFile);
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

    private boolean dispatchGrantCommand(String playerName, String group) {
        String template = config.getString("settings.command", "lp user %player% group set %group%");
        Map<String, String> variables = variables("console", playerName, group, getGroupDisplayName(group));
        String command = replaceVariables(template, variables);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void recordGrant(Player sender, Player target, String group) {
        String path = "players." + sender.getUniqueId();
        storage.set(path + ".name", sender.getName());
        storage.set(path + ".grants." + group, getUsedLimit(sender.getUniqueId(), group) + 1);

        List<Map<?, ?>> existingHistory = storage.getMapList(path + ".history");
        List<Map<String, Object>> history = new ArrayList<>();
        for (Map<?, ?> entry : existingHistory) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> value : entry.entrySet()) {
                if (value.getKey() != null) {
                    copy.put(value.getKey().toString(), value.getValue());
                }
            }
            history.add(copy);
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("target", target.getName());
        entry.put("target-uuid", target.getUniqueId().toString());
        entry.put("group", group);
        entry.put("at", Instant.now().toString());
        history.add(entry);
        storage.set(path + ".history", history);
    }

    private int getUsedLimit(UUID playerId, String group) {
        return Math.max(0, storage.getInt("players." + playerId + ".grants." + group));
    }

    private String getHighestConfiguredGroup(Player player) {
        String bestGroup = "default";
        int bestPriority = getGroupPriority(bestGroup);
        for (String group : getConfiguredGroups()) {
            if (!hasGroupPermission(player, group)) {
                continue;
            }

            int priority = getGroupPriority(group);
            if (priority >= bestPriority) {
                bestGroup = group;
                bestPriority = priority;
            }
        }
        return bestGroup;
    }

    private boolean hasGroupPermission(Player player, String group) {
        return player.hasPermission("group." + group)
                || player.hasPermission("luckperms.group." + group)
                || player.hasPermission("epicgranter.group." + group);
    }

    private int getGroupPriority(String group) {
        return config.getInt("priorities." + normalizeGroup(group), group.equalsIgnoreCase("default") ? 0 : -1);
    }

    private List<String> getGrantableGroups(CommandSender sender) {
        if (hasAdminAccess(sender) || !(sender instanceof Player player)) {
            return getConfiguredGroups();
        }

        String senderGroup = getHighestConfiguredGroup(player);
        ConfigurationSection limits = config.getConfigurationSection("limits." + senderGroup);
        if (limits == null) {
            return Collections.emptyList();
        }

        List<String> groups = new ArrayList<>();
        for (String group : limits.getKeys(false)) {
            groups.add(normalizeGroup(group));
        }
        return groups;
    }

    private List<String> getConfiguredGroups() {
        Set<String> groups = new LinkedHashSet<>();
        addSectionKeys(groups, "priorities");
        addSectionKeys(groups, "groups_names");
        ConfigurationSection limits = config == null ? null : config.getConfigurationSection("limits");
        if (limits != null) {
            for (String ownerGroup : limits.getKeys(false)) {
                groups.add(normalizeSimple(ownerGroup));
                ConfigurationSection grantable = limits.getConfigurationSection(ownerGroup);
                if (grantable != null) {
                    for (String group : grantable.getKeys(false)) {
                        groups.add(normalizeSimple(group));
                    }
                }
            }
        }
        return new ArrayList<>(groups);
    }

    private void addSectionKeys(Set<String> groups, String path) {
        ConfigurationSection section = config == null ? null : config.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            groups.add(normalizeSimple(key));
        }
    }

    private boolean isKnownGroup(String group) {
        return getConfiguredGroups().contains(normalizeGroup(group));
    }

    private String normalizeGroup(String input) {
        String normalized = normalizeSimple(input);
        for (String group : getConfiguredGroups()) {
            if (group.equalsIgnoreCase(normalized)) {
                return group;
            }
        }
        return normalized;
    }

    private String normalizeSimple(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private String getGroupDisplayName(String group) {
        return color(config.getString("groups_names." + normalizeGroup(group), group));
    }

    private boolean hasAdminAccess(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(ADMIN_PERMISSION);
    }

    private void sendHelp(CommandSender sender) {
        List<String> help = config.getStringList("settings.help");
        if (help.isEmpty()) {
            sendFallback(sender, "&6&lGRANT: &f/grant give <player> <group>");
            sendFallback(sender, "&6&lGRANT: &f/grant limits");
            return;
        }

        for (String line : help) {
            send(sender, replaceVariables(line, Map.of()));
        }
    }

    private void sendConfiguredMessage(CommandSender sender, String key, Map<String, String> variables) {
        String message = config.getString("message." + key);
        if (message == null || message.isBlank()) {
            return;
        }
        send(sender, replaceVariables(message, variables));
    }

    private void sendFallback(CommandSender sender, String message) {
        send(sender, message);
    }

    private void send(CommandSender sender, String message) {
        String colored = color(message);
        if (sender instanceof Player) {
            sender.sendMessage(colored);
        } else {
            sender.sendMessage(ChatColor.stripColor(colored));
        }
    }

    private String color(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replace("Â§", "&").replace(ChatColor.COLOR_CHAR, '&');
        return ChatColor.translateAlternateColorCodes('&', normalized);
    }

    private void sendGrantTitle(Map<String, String> variables) {
        if (!config.getBoolean("settings.titles.enabled", false)) {
            return;
        }

        String title = color(replaceVariables(config.getString("settings.titles.title", ""), variables));
        String subtitle = color(replaceVariables(config.getString("settings.titles.subtitle", ""), variables));
        int fadeIn = Math.max(0, config.getInt("settings.titles.fadeIn", 20));
        int stay = Math.max(0, config.getInt("settings.titles.stay", 40));
        int fadeOut = Math.max(0, config.getInt("settings.titles.fadeOut", 20));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    private Map<String, String> variables(String senderName, String targetName, String group, String groupName) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("0", senderName);
        variables.put("1", targetName);
        variables.put("2", groupName);
        variables.put("sender", senderName);
        variables.put("player", targetName);
        variables.put("target", targetName);
        variables.put("group", group);
        variables.put("group_name", groupName);
        return variables;
    }

    private String replaceVariables(String input, Map<String, String> variables) {
        String result = input == null ? "" : input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + entry.getKey() + "}", value);
            result = result.replace("%" + entry.getKey() + "%", value);
        }
        return result;
    }

    private List<String> filterSuggestions(List<String> values, String input) {
        String normalizedInput = normalizeSimple(input);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                result.add(value);
            }
        }
        return result;
    }
}
