package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EpicRewards extends JavaPlugin implements Listener, TabExecutor {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([wdhms])", Pattern.CASE_INSENSITIVE);

    private final Map<String, ChainDefinition> chains = new LinkedHashMap<>();
    private final Map<String, RewardDefinition> rewardCommands = new HashMap<>();
    private final Map<String, Command> registeredRewardCommands = new HashMap<>();
    private final Map<UUID, PlayerProgress> playerProgress = new HashMap<>();

    private File dataFile;
    private YamlConfiguration dataConfig;
    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDataFile();
        loadData();
        loadChains();
        registerRewardCommands();
        registerMainCommand();

        Bukkit.getPluginManager().registerEvents(this, this);

        long intervalSeconds = Math.max(1L, getConfig().getLong("check-interval-seconds", 30L));
        checkTask = Bukkit.getScheduler().runTaskTimer(this, this::tickOnlinePlayers, 20L, intervalSeconds * 20L);

        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            startSession(player, now, false);
        }
    }

    @Override
    public void onDisable() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        unregisterRewardCommands();

        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopSession(player, now);
        }
        saveData();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        startSession(event.getPlayer(), System.currentTimeMillis(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopSession(event.getPlayer(), System.currentTimeMillis());
        saveData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("epicrewards.admin")) {
                sender.sendMessage(color("&cNo permission."));
                return true;
            }

            reloadConfig();
            loadChains();
            registerRewardCommands();
            tickOnlinePlayers();
            sender.sendMessage(color("&aEpicRewards reloaded."));
            return true;
        }

        sender.sendMessage(color("&e/" + label + " reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("epicrewards.admin")) {
            return "reload".startsWith(args[0].toLowerCase(Locale.ROOT))
                    ? Collections.singletonList("reload")
                    : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private void registerMainCommand() {
        PluginCommand command = getCommand("epicrewards");
        if (command == null) {
            getLogger().warning("Command /epicrewards is missing in plugin.yml.");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void setupDataFile() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    getLogger().warning("Could not create playerdata.yml.");
                }
            } catch (IOException exception) {
                getLogger().log(Level.SEVERE, "Could not create playerdata.yml.", exception);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadChains() {
        chains.clear();
        rewardCommands.clear();

        List<?> rawChains = getConfig().getList("chains", Collections.emptyList());
        for (Object rawChain : rawChains) {
            Map<?, ?> chainMap = asMap(rawChain);
            if (chainMap == null) {
                continue;
            }

            String id = stringValue(chainMap.get("id"));
            if (id.isBlank()) {
                getLogger().warning("Skipped reward chain without id.");
                continue;
            }

            String mode = stringValue(chainMap.get("mode")).toLowerCase(Locale.ROOT);
            Map<?, ?> rawRewards = asMap(chainMap.get("rewards"));
            if (rawRewards == null || rawRewards.isEmpty()) {
                getLogger().warning("Skipped reward chain '" + id + "' because it has no rewards.");
                continue;
            }

            List<RewardDefinition> rewards = new ArrayList<>();
            for (Map.Entry<?, ?> entry : rawRewards.entrySet()) {
                int index = intValue(entry.getKey(), -1);
                Map<?, ?> rewardMap = asMap(entry.getValue());
                if (index < 0 || rewardMap == null) {
                    continue;
                }

                String command = normalizeCommand(stringValue(rewardMap.get("command")));
                if (command.isBlank()) {
                    getLogger().warning("Skipped reward " + index + " in chain '" + id + "' because command is empty.");
                    continue;
                }

                RewardDefinition reward = new RewardDefinition(
                        id,
                        index,
                        command,
                        parseDurationMillis(rewardMap.get("delay")),
                        stringList(rewardMap.get("actions")),
                        stringValue(rewardMap.get("direct-message")),
                        stringValue(rewardMap.get("broadcast")),
                        stringValue(rewardMap.get("not-ready")),
                        stringValue(rewardMap.get("already-given")),
                        stringValue(rewardMap.get("notification"))
                );
                rewards.add(reward);
            }

            rewards.sort(Comparator.comparingInt(reward -> reward.index));
            if (rewards.isEmpty()) {
                getLogger().warning("Skipped reward chain '" + id + "' because all rewards are invalid.");
                continue;
            }

            ChainDefinition chain = new ChainDefinition(id, mode, rewards);
            chains.put(id, chain);

            for (RewardDefinition reward : rewards) {
                RewardDefinition previous = rewardCommands.put(reward.command, reward);
                if (previous != null) {
                    getLogger().warning("Reward command /" + reward.command + " is used more than once.");
                }
            }
        }

        getLogger().info("Loaded " + chains.size() + " reward chain(s) and " + rewardCommands.size() + " command(s).");
    }

    private void registerRewardCommands() {
        unregisterRemovedRewardCommands();

        for (String commandName : rewardCommands.keySet()) {
            if (registeredRewardCommands.containsKey(commandName)) {
                continue;
            }

            Command command = new RewardCommand(commandName);
            boolean registered = Bukkit.getCommandMap().register(getName().toLowerCase(Locale.ROOT), command);
            registeredRewardCommands.put(commandName, command);
            if (!registered) {
                getLogger().warning("Could not register /" + commandName + ". Another plugin may already own this command.");
            }
        }
    }

    private void unregisterRemovedRewardCommands() {
        List<String> removed = new ArrayList<>();
        for (String commandName : registeredRewardCommands.keySet()) {
            if (!rewardCommands.containsKey(commandName)) {
                removed.add(commandName);
            }
        }

        for (String commandName : removed) {
            Command command = registeredRewardCommands.remove(commandName);
            unregisterRewardCommand(command);
        }
    }

    private void unregisterRewardCommands() {
        for (Command command : registeredRewardCommands.values()) {
            unregisterRewardCommand(command);
        }
        registeredRewardCommands.clear();
    }

    private void unregisterRewardCommand(Command command) {
        if (command == null) {
            return;
        }

        command.unregister(Bukkit.getCommandMap());
        Bukkit.getCommandMap().getKnownCommands().entrySet().removeIf(entry -> entry.getValue() == command);
    }

    private boolean handleRewardCommand(CommandSender sender, String label) {
        RewardDefinition reward = rewardCommands.get(normalizeCommand(label));
        if (reward == null) {
            sender.sendMessage(color("&cReward command is not configured."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can claim rewards."));
            return true;
        }

        ChainDefinition chain = chains.get(reward.chainId);
        if (chain == null) {
            sender.sendMessage(color("&cReward chain is not loaded."));
            return true;
        }

        long now = System.currentTimeMillis();
        PlayerProgress progress = progress(player);
        progress.name = player.getName();
        ensureCurrentPeriod(progress, chain, now, player, true);

        ChainState state = progress.state(chain.id);
        if (chain.isComplete(state)) {
            sendConfiguredMessage(player, globalMessage("chain-already-given", reward.alreadyGiven), reward, 0L);
            return true;
        }

        if (state.claimed.contains(reward.index)) {
            sendConfiguredMessage(player, reward.alreadyGiven, reward, 0L);
            return true;
        }

        RewardDefinition previous = chain.previousReward(reward.index);
        if (previous != null && !state.claimed.contains(previous.index)) {
            sendConfiguredMessage(player, globalMessage("depends", "&cClaim the previous reward first."), reward, 0L);
            return true;
        }

        long playtime = playtimeMillis(progress, state, now);
        if (playtime < reward.delayMillis) {
            sendConfiguredMessage(player, reward.notReady, reward, reward.delayMillis - playtime);
            return true;
        }

        for (String action : reward.actions) {
            String command = placeholders(action, player, reward, 0L);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        state.claimed.add(reward.index);
        state.notified.add(reward.index);

        sendConfiguredMessage(player, reward.directMessage, reward, 0L);
        broadcastConfiguredMessage(reward.broadcast, player, reward);
        checkNotifications(player, now);
        saveData();
        return true;
    }

    private void tickOnlinePlayers() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProgress progress = progress(player);
            progress.name = player.getName();
            if (progress.sessionStartedAt <= 0L) {
                progress.sessionStartedAt = now;
                changed = true;
            }

            for (ChainDefinition chain : chains.values()) {
                changed |= ensureCurrentPeriod(progress, chain, now, player, true);
            }
            changed |= checkNotifications(player, now);
        }

        if (changed) {
            saveData();
        }
    }

    private void startSession(Player player, long now, boolean notifyReset) {
        PlayerProgress progress = progress(player);
        progress.name = player.getName();
        progress.sessionStartedAt = now;

        boolean changed = false;
        for (ChainDefinition chain : chains.values()) {
            changed |= ensureCurrentPeriod(progress, chain, now, notifyReset ? player : null, notifyReset);
        }
        changed |= checkNotifications(player, now);

        if (changed) {
            saveData();
        }
    }

    private void stopSession(Player player, long now) {
        PlayerProgress progress = progress(player);
        progress.name = player.getName();

        for (ChainDefinition chain : chains.values()) {
            ChainState state = progress.state(chain.id);
            ensureCurrentPeriod(progress, chain, now, null, false);
            state.playtimeMillis = playtimeMillis(progress, state, now);
        }
        progress.sessionStartedAt = 0L;
    }

    private boolean checkNotifications(Player player, long now) {
        PlayerProgress progress = progress(player);
        boolean changed = false;

        for (ChainDefinition chain : chains.values()) {
            ChainState state = progress.state(chain.id);
            RewardDefinition reward = chain.firstUnclaimed(state);
            if (reward == null || state.notified.contains(reward.index)) {
                continue;
            }

            long playtime = playtimeMillis(progress, state, now);
            if (playtime >= reward.delayMillis) {
                sendConfiguredMessage(player, reward.notification, reward, 0L);
                state.notified.add(reward.index);
                changed = true;
            }
        }

        return changed;
    }

    private boolean ensureCurrentPeriod(PlayerProgress progress, ChainDefinition chain, long now, Player player, boolean notify) {
        ChainState state = progress.state(chain.id);
        if (!chain.mode.equals("reset-next-week")) {
            if (state.periodStart <= 0L) {
                state.periodStart = now;
                return true;
            }
            return false;
        }

        long currentWeekStart = weekStartMillis(now);
        if (state.periodStart <= 0L) {
            state.periodStart = currentWeekStart;
            return true;
        }

        if (state.periodStart >= currentWeekStart) {
            return false;
        }

        boolean hadProgress = state.playtimeMillis > 0L || !state.claimed.isEmpty() || !state.notified.isEmpty();
        state.periodStart = currentWeekStart;
        state.playtimeMillis = 0L;
        state.claimed.clear();
        state.notified.clear();

        if (hadProgress && player != null && notify) {
            sendResetComplete(player);
        }
        return true;
    }

    private PlayerProgress progress(Player player) {
        return playerProgress.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerProgress(player.getName()));
    }

    private long playtimeMillis(PlayerProgress progress, ChainState state, long now) {
        long total = state.playtimeMillis;
        if (progress.sessionStartedAt > 0L) {
            long from = Math.max(progress.sessionStartedAt, state.periodStart);
            total += Math.max(0L, now - from);
        }
        return total;
    }

    private void sendResetComplete(Player player) {
        String chat = getConfig().getString("reset-complete.chat", "");
        String title = getConfig().getString("reset-complete.title", "");

        if (!chat.isBlank()) {
            sendMessage(player, placeholders(chat, player, null, 0L));
        }
        if (!title.isBlank()) {
            player.sendTitle(color(placeholders(title, player, null, 0L)), "", 10, 60, 20);
        }
    }

    private void sendConfiguredMessage(Player player, String raw, RewardDefinition reward, long remainsMillis) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        sendMessage(player, placeholders(raw, player, reward, remainsMillis));
    }

    private void broadcastConfiguredMessage(String raw, Player player, RewardDefinition reward) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        String message = placeholders(raw, player, reward, 0L);
        for (String line : message.split("\\R", -1)) {
            Bukkit.broadcastMessage(color(line));
        }
    }

    private void sendMessage(Player player, String raw) {
        for (String line : raw.split("\\R", -1)) {
            player.sendMessage(color(line));
        }
    }

    private String placeholders(String text, Player player, RewardDefinition reward, long remainsMillis) {
        if (text == null) {
            return "";
        }

        String result = text
                .replace("{player}", player.getName())
                .replace("{player_name}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{remains}", formatDuration(remainsMillis))
                .replace("{time}", formatDuration(remainsMillis));

        if (reward != null) {
            result = result
                    .replace("{chain}", reward.chainId)
                    .replace("{reward}", String.valueOf(reward.index))
                    .replace("{delay}", formatDuration(reward.delayMillis));
        }
        return result;
    }

    private String globalMessage(String path, String fallback) {
        return getConfig().getString(path, fallback == null ? "" : fallback);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private long weekStartMillis(long now) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate monday = Instant.ofEpochMilli(now)
                .atZone(zone)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private long parseDurationMillis(Object raw) {
        if (raw instanceof Number number) {
            return Math.max(0L, number.longValue() * 1000L);
        }

        String text = stringValue(raw).toLowerCase(Locale.ROOT).replace(" ", "");
        if (text.isBlank()) {
            return 0L;
        }

        if (text.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(text) * 1000L;
        }

        long millis = 0L;
        Matcher matcher = DURATION_PART.matcher(text);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            millis += switch (unit) {
                case "w" -> value * 7L * 24L * 60L * 60L * 1000L;
                case "d" -> value * 24L * 60L * 60L * 1000L;
                case "h" -> value * 60L * 60L * 1000L;
                case "m" -> value * 60L * 1000L;
                case "s" -> value * 1000L;
                default -> 0L;
            };
        }
        return Math.max(0L, millis);
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        if (seconds <= 0L) {
            return "0с";
        }

        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "д");
        }
        if (hours > 0L) {
            parts.add(hours + "ч");
        }
        if (minutes > 0L) {
            parts.add(minutes + "м");
        }
        if (seconds > 0L || parts.isEmpty()) {
            parts.add(seconds + "с");
        }

        return String.join(" ", parts);
    }

    private String normalizeCommand(String command) {
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void loadData() {
        playerProgress.clear();
        ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String uuidKey : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection playerSection = players.getConfigurationSection(uuidKey);
            if (playerSection == null) {
                continue;
            }

            PlayerProgress progress = new PlayerProgress(playerSection.getString("name", ""));
            ConfigurationSection chainsSection = playerSection.getConfigurationSection("chains");
            if (chainsSection != null) {
                for (String chainId : chainsSection.getKeys(false)) {
                    ConfigurationSection chainSection = chainsSection.getConfigurationSection(chainId);
                    if (chainSection == null) {
                        continue;
                    }

                    ChainState state = new ChainState();
                    state.periodStart = chainSection.getLong("period-start", 0L);
                    state.playtimeMillis = chainSection.getLong("playtime-ms", 0L);
                    state.claimed.addAll(chainSection.getIntegerList("claimed"));
                    state.notified.addAll(chainSection.getIntegerList("notified"));
                    progress.chains.put(chainId, state);
                }
            }
            playerProgress.put(uuid, progress);
        }
    }

    private void saveData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        long now = System.currentTimeMillis();
        dataConfig.set("players", null);

        for (Map.Entry<UUID, PlayerProgress> entry : playerProgress.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerProgress progress = entry.getValue();
            dataConfig.set(base + ".name", progress.name);

            for (Map.Entry<String, ChainState> chainEntry : progress.chains.entrySet()) {
                String chainPath = base + ".chains." + chainEntry.getKey();
                ChainState state = chainEntry.getValue();
                dataConfig.set(chainPath + ".period-start", state.periodStart);
                dataConfig.set(chainPath + ".playtime-ms", playtimeMillis(progress, state, now));
                dataConfig.set(chainPath + ".claimed", new ArrayList<>(state.claimed));
                dataConfig.set(chainPath + ".notified", new ArrayList<>(state.notified));
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save playerdata.yml.", exception);
        }
    }

    private Map<?, ?> asMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return map;
        }
        if (raw instanceof ConfigurationSection section) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                values.put(key, section.get(key));
            }
            return values;
        }
        return null;
    }

    private String stringValue(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private int intValue(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(stringValue(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<String> stringList(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                String value = stringValue(item);
                if (!value.isBlank()) {
                    result.add(value);
                }
            }
            return result;
        }

        String value = stringValue(raw);
        return value.isBlank() ? Collections.emptyList() : Collections.singletonList(value);
    }

    private final class RewardCommand extends Command {
        private RewardCommand(String name) {
            super(name);
            setDescription("EpicRewards reward command");
            setUsage("/" + name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return handleRewardCommand(sender, getName());
        }
    }

    private static final class ChainDefinition {
        private final String id;
        private final String mode;
        private final List<RewardDefinition> rewards;

        private ChainDefinition(String id, String mode, List<RewardDefinition> rewards) {
            this.id = id;
            this.mode = mode;
            this.rewards = List.copyOf(rewards);
        }

        private RewardDefinition previousReward(int index) {
            RewardDefinition previous = null;
            for (RewardDefinition reward : rewards) {
                if (reward.index >= index) {
                    return previous;
                }
                previous = reward;
            }
            return previous;
        }

        private RewardDefinition firstUnclaimed(ChainState state) {
            for (RewardDefinition reward : rewards) {
                if (!state.claimed.contains(reward.index)) {
                    return reward;
                }
            }
            return null;
        }

        private boolean isComplete(ChainState state) {
            return firstUnclaimed(state) == null;
        }
    }

    private static final class RewardDefinition {
        private final String chainId;
        private final int index;
        private final String command;
        private final long delayMillis;
        private final List<String> actions;
        private final String directMessage;
        private final String broadcast;
        private final String notReady;
        private final String alreadyGiven;
        private final String notification;

        private RewardDefinition(
                String chainId,
                int index,
                String command,
                long delayMillis,
                List<String> actions,
                String directMessage,
                String broadcast,
                String notReady,
                String alreadyGiven,
                String notification
        ) {
            this.chainId = chainId;
            this.index = index;
            this.command = command;
            this.delayMillis = delayMillis;
            this.actions = List.copyOf(actions);
            this.directMessage = directMessage;
            this.broadcast = broadcast;
            this.notReady = notReady;
            this.alreadyGiven = alreadyGiven;
            this.notification = notification;
        }
    }

    private static final class PlayerProgress {
        private String name;
        private long sessionStartedAt;
        private final Map<String, ChainState> chains = new HashMap<>();

        private PlayerProgress(String name) {
            this.name = name;
        }

        private ChainState state(String chainId) {
            return chains.computeIfAbsent(chainId, ignored -> new ChainState());
        }
    }

    private static final class ChainState {
        private long periodStart;
        private long playtimeMillis;
        private final Set<Integer> claimed = new LinkedHashSet<>();
        private final Set<Integer> notified = new LinkedHashSet<>();
    }
}
