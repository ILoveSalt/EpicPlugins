package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class StaffService {
    private static final String SPEC_PERMISSION = "epicstaffcontrol.spec";
    private static final Set<String> DEFAULT_MODERATOR_RANKS = Set.of(
            "moderator",
            "senior-moderator",
            "administrator"
    );
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final EpicStaffControl plugin;
    private final StaffRepository repository;
    private final AnimationSupport animationSupport;
    private final LegacyComponentSerializer messageSerializer;
    private final Map<String, RankDefinition> ranks = new LinkedHashMap<>();
    private YamlConfiguration config;
    private YamlConfiguration returnRanks;
    private Set<String> punishmentCommands = Set.of();
    private Set<String> specNotifyRanks = DEFAULT_MODERATOR_RANKS;

    public StaffService(EpicStaffControl plugin) {
        this.plugin = plugin;
        this.animationSupport = new AnimationSupport(plugin);
        this.messageSerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        ensureResources();
        reloadConfiguration();
        this.repository = new StaffRepository(plugin);
    }

    public Collection<RankDefinition> getRanks() {
        return Collections.unmodifiableCollection(ranks.values());
    }

    public RankDefinition getRank(String id) {
        if (id == null) {
            return null;
        }
        return ranks.get(id.toLowerCase(Locale.ROOT));
    }

    public StaffProfile getProfile(Player player) {
        return repository.getOrCreate(player.getUniqueId(), player.getName());
    }

    public StaffProfile getProfile(OfflinePlayer player) {
        return repository.getOrCreate(player.getUniqueId(), player.getName());
    }

    public boolean hasStaffRank(StaffProfile profile) {
        return profile != null && getRank(profile.getRank()) != null;
    }

    public boolean canReceiveSpec(Player player) {
        return player.hasPermission(SPEC_PERMISSION) || hasConfiguredRank(player, specNotifyRanks);
    }

    public void requestSpec(Player requester) {
        Map<String, String> variables = baseVariables(requester.getName(), null);
        sendConfigured(requester, "messages.spec-request-sent", variables);

        String message = config.getString("messages.spec-request", "");
        if (message.isBlank()) {
            return;
        }

        Component hover = renderComponent(
                config.getString("messages.spec-hover", "&a/spec %player%"),
                variables,
                requester
        );
        Component rendered = renderComponent(message, variables, requester)
                .clickEvent(ClickEvent.runCommand("/spec " + requester.getName()))
                .hoverEvent(HoverEvent.showText(hover));

        Bukkit.getConsoleSender().sendMessage(rendered);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canReceiveSpec(online)) {
                online.sendMessage(rendered);
            }
        }
    }

    public void teleportToSpecTarget(Player staff, Player target) {
        if (!canReceiveSpec(staff)) {
            sendConfigured(staff, "messages.no-permission", Map.of());
            return;
        }

        staff.teleport(target.getLocation());
        sendConfigured(staff, "messages.spec-teleported", Map.of("player", target.getName()));
    }

    public void giveRank(CommandSender sender, OfflinePlayer target, RankDefinition rank) {
        StaffProfile profile = getProfile(target);
        profile.finishShift(now());
        profile.setRank(rank.id());
        profile.setWorkedSeconds(0);
        profile.setPunishments(0);
        profile.setLastSalaryAt(0);

        dispatch(rank.giveCommand(), target.getName(), rank, getReturnGroup(target));
        repository.save();

        Map<String, String> variables = baseVariables(target.getName(), rank);
        variables.put("admin", sender.getName());
        sendConfigured(sender, "messages.rank-given", variables);
    }

    public void takeRank(CommandSender sender, OfflinePlayer target) {
        StaffProfile profile = repository.get(target.getUniqueId());
        RankDefinition rank = profile == null ? null : getRank(profile.getRank());
        if (rank == null) {
            sendConfigured(sender, "messages.no-rank", Map.of("player", safeName(target)));
            return;
        }

        profile.finishShift(now());
        String returnGroup = getReturnGroup(target);
        dispatch(rank.takeCommand(), target.getName(), rank, returnGroup);
        repository.remove(target.getUniqueId());
        repository.save();

        Map<String, String> variables = baseVariables(safeName(target), rank);
        variables.put("admin", sender.getName());
        variables.put("return_group", returnGroup);
        sendConfigured(sender, "messages.rank-taken", variables);
    }

    public void startWork(Player player) {
        StaffProfile profile = getProfile(player);
        RankDefinition rank = getRank(profile.getRank());
        if (rank == null) {
            sendConfigured(player, "messages.no-rank", Map.of("player", player.getName()));
            return;
        }
        if (profile.isOnDuty()) {
            sendConfigured(player, "messages.already-working", baseVariables(player.getName(), rank));
            return;
        }

        profile.setShiftStartedAt(now());
        repository.save();

        Map<String, String> variables = baseVariables(player.getName(), rank);
        variables.put("time", clockTime());
        send(player, rank.workOnMessage(), variables);
        notifyStaff("messages.work-started", variables);
    }

    public void stopWork(Player player, boolean sendMessages) {
        StaffProfile profile = repository.get(player.getUniqueId());
        RankDefinition rank = profile == null ? null : getRank(profile.getRank());
        if (rank == null) {
            if (sendMessages) {
                sendConfigured(player, "messages.no-rank", Map.of("player", player.getName()));
            }
            return;
        }
        if (!profile.isOnDuty()) {
            if (sendMessages) {
                sendConfigured(player, "messages.not-working", baseVariables(player.getName(), rank));
            }
            return;
        }

        profile.finishShift(now());
        repository.save();

        if (sendMessages) {
            Map<String, String> variables = baseVariables(player.getName(), rank);
            variables.put("time", clockTime());
            send(player, rank.workOffMessage(), variables);
            variables.put("worked_time", formatDuration(profile.getWorkedSeconds()));
            notifyStaff("messages.work-stopped", variables);
        }
    }

    public void paySalary(Player player) {
        StaffProfile profile = getProfile(player);
        RankDefinition rank = getRank(profile.getRank());
        if (rank == null) {
            sendConfigured(player, "messages.no-rank", Map.of("player", player.getName()));
            return;
        }
        if (config.getBoolean("settings.salary-requires-duty", true) && !profile.isOnDuty()) {
            sendConfigured(player, "messages.salary-requires-duty", baseVariables(player.getName(), rank));
            return;
        }

        long now = now();
        long availableAt = profile.getLastSalaryAt() + rank.salaryCooldownSeconds();
        if (profile.getLastSalaryAt() > 0 && now < availableAt) {
            Map<String, String> variables = baseVariables(player.getName(), rank);
            variables.put("time", formatDuration(availableAt - now));
            send(player, rank.salaryCooldownMessage(), variables);
            return;
        }

        if (!dispatch(rank.salaryCommand(), player.getName(), rank, getReturnGroup(player))) {
            sendConfigured(player, "messages.command-failed", baseVariables(player.getName(), rank));
            return;
        }

        profile.setLastSalaryAt(now);
        repository.save();

        Map<String, String> variables = baseVariables(player.getName(), rank);
        send(player, rank.salaryMessage(), variables);
        notifyStaff("messages.salary-received", variables);
    }

    public void promote(Player player) {
        StaffProfile profile = getProfile(player);
        RankDefinition currentRank = getRank(profile.getRank());
        if (currentRank == null) {
            sendConfigured(player, "messages.no-rank", Map.of("player", player.getName()));
            return;
        }

        RankDefinition nextRank = getRank(currentRank.nextRank());
        long workedSeconds = profile.getTotalWorkedSeconds(now());
        boolean eligible = nextRank != null
                && workedSeconds >= currentRank.requiredWorkSeconds()
                && profile.getPunishments() >= currentRank.requiredPunishments();

        if (!eligible) {
            Map<String, String> variables = promotionVariables(player.getName(), currentRank, nextRank, profile);
            send(player, currentRank.promotionDeniedMessage(), variables);
            return;
        }

        profile.setRank(nextRank.id());
        dispatch(nextRank.giveCommand(), player.getName(), nextRank, getReturnGroup(player));
        repository.save();

        Map<String, String> variables = promotionVariables(player.getName(), currentRank, nextRank, profile);
        for (String message : currentRank.promotionMessages()) {
            send(player, message, variables);
        }
        notifyStaff("messages.rank-up", variables);
    }

    public void sendRankInfo(CommandSender sender, RankDefinition rank) {
        if (rank == null) {
            sendConfigured(sender, "messages.invalid-rank", Map.of("rank", "?"));
            return;
        }
        Map<String, String> variables = baseVariables(sender.getName(), rank);
        for (String line : rank.infoMessages()) {
            send(sender, line, variables);
        }
    }

    public void sendRankList(CommandSender sender) {
        List<String> configured = config.getStringList("messages.rank-list");
        if (!configured.isEmpty()) {
            for (String line : configured) {
                send(sender, line, Map.of());
            }
            return;
        }

        send(sender, "%prefix%§aСписок должностей:", Map.of());
        for (RankDefinition rank : ranks.values()) {
            send(sender, "§e- " + rank.displayName() + " §7(" + rank.id() + ")", Map.of());
        }
    }

    public void sendStats(CommandSender sender, OfflinePlayer target) {
        StaffProfile profile = repository.get(target.getUniqueId());
        RankDefinition rank = profile == null ? null : getRank(profile.getRank());
        if (profile == null || rank == null) {
            sendConfigured(sender, "messages.no-rank", Map.of("player", safeName(target)));
            return;
        }

        Map<String, String> variables = baseVariables(safeName(target), rank);
        variables.put("worked_time", formatDuration(profile.getTotalWorkedSeconds(now())));
        variables.put("punishments", Integer.toString(profile.getPunishments()));
        variables.put("status", profile.isOnDuty()
                ? config.getString("messages.status-working", "§aработает")
                : config.getString("messages.status-offline", "§7не на смене"));

        for (String line : config.getStringList("messages.stats")) {
            send(sender, line, variables);
        }
    }

    public void setPunishments(CommandSender sender, OfflinePlayer target, int amount) {
        StaffProfile profile = repository.get(target.getUniqueId());
        RankDefinition rank = profile == null ? null : getRank(profile.getRank());
        if (profile == null || rank == null) {
            sendConfigured(sender, "messages.no-rank", Map.of("player", safeName(target)));
            return;
        }

        profile.setPunishments(amount);
        repository.save();
        Map<String, String> variables = baseVariables(safeName(target), rank);
        variables.put("punishments", Integer.toString(profile.getPunishments()));
        sendConfigured(sender, "messages.punishments-set", variables);
    }

    public void recordPunishment(Player player, String rawCommand) {
        String commandName = extractCommandName(rawCommand);
        if (!punishmentCommands.contains(commandName)) {
            return;
        }

        StaffProfile profile = repository.get(player.getUniqueId());
        RankDefinition rank = profile == null ? null : getRank(profile.getRank());
        if (profile == null || rank == null || !profile.isOnDuty()) {
            return;
        }

        profile.setPunishments(profile.getPunishments() + 1);
        repository.save();

        Map<String, String> variables = baseVariables(player.getName(), rank);
        variables.put("punishments", Integer.toString(profile.getPunishments()));
        sendConfigured(player, "messages.punishment-counted", variables);
    }

    public boolean reload(CommandSender sender) {
        try {
            reloadConfiguration();
            sendConfigured(sender, "messages.reloaded", Map.of());
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Could not reload configuration: " + exception.getMessage());
            sendConfigured(sender, "messages.reload-failed", Map.of("error", exception.getMessage()));
            return false;
        }
    }

    public void shutdown() {
        long now = now();
        for (StaffProfile profile : repository.all()) {
            profile.finishShift(now);
        }
        repository.save();
    }

    public String normalizeRankId(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.toLowerCase(Locale.ROOT);
        if (ranks.containsKey(normalized)) {
            return normalized;
        }
        for (RankDefinition rank : ranks.values()) {
            if (rank.displayName().equalsIgnoreCase(input)) {
                return rank.id();
            }
        }
        return normalized;
    }

    public void sendConfigured(CommandSender sender, String path, Map<String, String> variables) {
        String message = config.getString(path);
        if (message != null && !message.isBlank()) {
            send(sender, message, variables);
        }
    }

    private void ensureResources() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }
        saveResourceIfMissing("Config.yml");
        saveResourceIfMissing("ranks.yml");
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.isFile()) {
            plugin.saveResource(name, false);
        }
    }

    private void reloadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "Config.yml");
        File lowercaseConfig = new File(plugin.getDataFolder(), "config.yml");
        if (lowercaseConfig.isFile()) {
            configFile = lowercaseConfig;
        }

        YamlConfiguration loadedConfig = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration loadedReturnRanks =
                YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "ranks.yml"));
        Map<String, RankDefinition> loadedRanks = loadRanks(loadedConfig);
        Set<String> loadedPunishmentCommands = loadedConfig.getStringList("settings.punishment-commands").stream()
                .map(this::normalizeCommandName)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> loadedSpecNotifyRanks = loadRankSet(loadedConfig, "settings.spec.notify-ranks");

        config = loadedConfig;
        returnRanks = loadedReturnRanks;
        ranks.clear();
        ranks.putAll(loadedRanks);
        punishmentCommands = loadedPunishmentCommands;
        specNotifyRanks = loadedSpecNotifyRanks;
    }

    private Set<String> loadRankSet(YamlConfiguration source, String path) {
        Set<String> loaded = source.getStringList(path).stream()
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return loaded.isEmpty() ? DEFAULT_MODERATOR_RANKS : loaded;
    }

    private Map<String, RankDefinition> loadRanks(YamlConfiguration source) {
        ConfigurationSection groups = source.getConfigurationSection("groups");
        if (groups == null || groups.getKeys(false).isEmpty()) {
            throw new IllegalStateException("Config.yml does not contain any groups");
        }

        Map<String, RankDefinition> loaded = new LinkedHashMap<>();
        for (String rawId : groups.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            String path = "groups." + rawId;
            RankDefinition rank = new RankDefinition(
                    id,
                    source.getString(path + ".display-name", rawId),
                    source.getString(path + ".command-give", ""),
                    source.getString(path + ".command-take", ""),
                    source.getString(path + ".command-salary", ""),
                    source.getLong(path + ".salary-amount"),
                    Math.max(0, source.getLong(path + ".command-cooldown-salary")),
                    source.getString(path + ".command-salary-message", ""),
                    source.getString(path + ".command-salary-cooldown-message", ""),
                    source.getString(path + ".command-on", ""),
                    source.getString(path + ".command-off", ""),
                    List.copyOf(source.getStringList(path + ".command-info")),
                    List.copyOf(source.getStringList(path + ".command-up")),
                    source.getString(path + ".command-deny-up", ""),
                    source.getString(path + ".next-rank", "").toLowerCase(Locale.ROOT),
                    Math.max(0, source.getLong(path + ".required-work-seconds")),
                    Math.max(0, source.getInt(path + ".required-punishments"))
            );
            loaded.put(id, rank);
        }

        for (RankDefinition rank : loaded.values()) {
            if (!rank.nextRank().isBlank() && !loaded.containsKey(rank.nextRank())) {
                throw new IllegalStateException(
                        "Rank '" + rank.id() + "' references missing next-rank '" + rank.nextRank() + "'"
                );
            }
        }

        return loaded;
    }

    private boolean dispatch(String template, String playerName, RankDefinition rank, String returnGroup) {
        if (template == null || template.isBlank()) {
            return true;
        }

        Map<String, String> variables = baseVariables(playerName, rank);
        variables.put("return_group", returnGroup);
        String command = replaceVariables(template, variables)
                .replace("\"player\"", quote(playerName));
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private String getReturnGroup(OfflinePlayer player) {
        List<String> candidates = new ArrayList<>();
        candidates.add("players." + player.getUniqueId());
        if (player.getName() != null) {
            candidates.add("players." + player.getName());
            candidates.add("ranks." + player.getName());
            candidates.add(player.getName());
        }
        candidates.add("ranks." + player.getUniqueId());
        candidates.add(player.getUniqueId().toString());

        for (String path : candidates) {
            String value = returnRanks.getString(path);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return config.getString("settings.default-return-group", "default");
    }

    private Map<String, String> promotionVariables(
            String playerName,
            RankDefinition currentRank,
            RankDefinition nextRank,
            StaffProfile profile
    ) {
        Map<String, String> variables = baseVariables(playerName, nextRank == null ? currentRank : nextRank);
        variables.put("current_rank", currentRank.displayName());
        variables.put("next_rank", nextRank == null ? currentRank.displayName() : nextRank.displayName());
        variables.put("worked_time", formatDuration(profile.getTotalWorkedSeconds(now())));
        variables.put("required_work_time", formatDuration(currentRank.requiredWorkSeconds()));
        variables.put("punishments", Integer.toString(profile.getPunishments()));
        variables.put("required_punishments", Integer.toString(currentRank.requiredPunishments()));
        return variables;
    }

    private Map<String, String> baseVariables(String playerName, RankDefinition rank) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("player", playerName == null ? "unknown" : playerName);
        variables.put("rank", rank == null ? "" : rank.displayName());
        variables.put("rank_id", rank == null ? "" : rank.id());
        variables.put("amount", rank == null ? "0" : Long.toString(rank.salaryAmount()));
        return variables;
    }

    private boolean hasConfiguredRank(Player player, Set<String> allowedRanks) {
        StaffProfile profile = repository.get(player.getUniqueId());
        if (profile == null) {
            return false;
        }
        return allowedRanks.contains(profile.getRank().toLowerCase(Locale.ROOT));
    }

    private void notifyStaff(String path, Map<String, String> variables) {
        String message = config.getString(path);
        if (message == null || message.isBlank()) {
            return;
        }

        Player animationSource = findAnimationSource(variables, null);
        Component rendered = renderComponent(message, variables, animationSource);
        Bukkit.getConsoleSender().sendMessage(rendered);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("epicstaffcontrol.notify")) {
                online.sendMessage(rendered);
            }
        }
    }

    private void send(CommandSender sender, String message, Map<String, String> variables) {
        if (message != null && !message.isBlank()) {
            Player fallback = sender instanceof Player player ? player : null;
            Player animationSource = findAnimationSource(variables, fallback);
            sender.sendMessage(renderComponent(message, variables, animationSource));
        }
    }

    private Component renderComponent(
            String message,
            Map<String, String> variables,
            Player animationSource
    ) {
        String rendered = message.replace("%prefix%", config.getString("settings.prefix", ""));
        rendered = replaceVariables(rendered, variables);
        rendered = rendered.replace('§', '&');
        Component component = messageSerializer.deserialize(rendered);
        return animationSupport.replace(component, animationSource);
    }

    private Player findAnimationSource(Map<String, String> variables, Player fallback) {
        String playerName = variables.get("player");
        if (playerName == null || playerName.isBlank()) {
            return fallback;
        }

        Player player = Bukkit.getPlayerExact(playerName);
        return player == null ? fallback : player;
    }

    private String replaceVariables(String input, Map<String, String> variables) {
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value);
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    private String extractCommandName(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        int separator = command.indexOf(' ');
        if (separator >= 0) {
            command = command.substring(0, separator);
        }
        return normalizeCommandName(command);
    }

    private String normalizeCommandName(String command) {
        String normalized = command.toLowerCase(Locale.ROOT).trim();
        int namespace = normalized.indexOf(':');
        return namespace >= 0 ? normalized.substring(namespace + 1) : normalized;
    }

    private String formatDuration(long totalSeconds) {
        long safeSeconds = Math.max(0, totalSeconds);
        long days = safeSeconds / 86_400;
        long hours = safeSeconds % 86_400 / 3_600;
        long minutes = safeSeconds % 3_600 / 60;
        long seconds = safeSeconds % 60;

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + " д.");
        }
        if (hours > 0) {
            parts.add(hours + " ч.");
        }
        if (minutes > 0) {
            parts.add(minutes + " мин.");
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(seconds + " сек.");
        }
        return String.join(" ", parts);
    }

    private String clockTime() {
        ZoneId zone = ZoneId.systemDefault();
        String configured = config.getString("settings.time-zone", "system");
        if (configured != null && !configured.equalsIgnoreCase("system")) {
            try {
                zone = ZoneId.of(configured);
            } catch (Exception ignored) {
                plugin.getLogger().warning("Unknown time zone '" + configured + "', using system time zone.");
            }
        }
        return LocalTime.ofInstant(Instant.now(), zone).format(CLOCK_FORMAT);
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private String quote(String value) {
        return "\"" + value.replace("\"", "") + "\"";
    }
}
