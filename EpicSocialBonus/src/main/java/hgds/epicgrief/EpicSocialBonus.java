package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class EpicSocialBonus extends JavaPlugin implements CommandExecutor {
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final List<String> DEFAULT_TG_COMMANDS = List.of("/account reward tg", "/tg");

    private final Object dataLock = new Object();
    private File dataFile;
    private FileConfiguration playerData;
    private TelegramPoller telegramPoller;
    private ExecutorService telegramSender;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupPlayerData();

        registerCommand("free");
        registerCommand("tgbonus");
        registerCommand("epicsocialbonus");

        telegramSender = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "EpicSocialBonus-TelegramSender");
            thread.setDaemon(true);
            return thread;
        });

        startTelegramBot();
    }

    @Override
    public void onDisable() {
        stopTelegramBot();
        if (telegramSender != null) {
            telegramSender.shutdownNow();
        }
        synchronized (dataLock) {
            savePlayerDataLocked();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("epicsocialbonus")) {
            return handleAdminCommand(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(message("messages.onlyPlayer", "&cThis command is only for players.", null)));
            return true;
        }

        if (hasRewardForPlayer(player.getName())) {
            player.sendMessage(color(message("messages.alreadyTG", "&cYou have already received this reward.", player.getName())));
            return true;
        }

        player.sendMessage(color(message("messages.commandUsage", "&fUse /tg <nick> in Telegram.", player.getName())));
        return true;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("epicsocialbonus.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            reloadConfig();
            synchronized (dataLock) {
                playerData = YamlConfiguration.loadConfiguration(dataFile);
                ensureDataDefaultsLocked();
            }
            stopTelegramBot();
            startTelegramBot();
            sender.sendMessage(ChatColor.GREEN + "EpicSocialBonus reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/epicsocialbonus reload");
        return true;
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(this);
        }
    }

    private void setupPlayerData() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            saveResource("playerdata.yml", false);
        }

        synchronized (dataLock) {
            playerData = YamlConfiguration.loadConfiguration(dataFile);
            ensureDataDefaultsLocked();
            savePlayerDataLocked();
        }
    }

    private void ensureDataDefaultsLocked() {
        if (!playerData.isConfigurationSection("players")) {
            playerData.createSection("players");
        }
        if (!playerData.isConfigurationSection("telegram-users")) {
            playerData.createSection("telegram-users");
        }
        if (!playerData.isConfigurationSection("meta")) {
            playerData.createSection("meta");
        }
    }

    private void startTelegramBot() {
        if (!getConfig().getBoolean("telegram.enabled", true)) {
            getLogger().info("Telegram polling is disabled in config.yml.");
            return;
        }

        String token = getConfig().getString("tg_settings.token", "").trim();
        if (token.isEmpty()) {
            getLogger().warning("Telegram bot token is empty. Set tg_settings.token in config.yml.");
            return;
        }

        telegramPoller = new TelegramPoller(token);
        telegramPoller.start();
        getLogger().info("Telegram polling started.");
    }

    private void stopTelegramBot() {
        if (telegramPoller != null) {
            telegramPoller.stop();
            telegramPoller = null;
        }
    }

    private boolean handleTelegramMessage(TelegramMessage message) {
        String text = stripBotMention(message.text()).trim();
        CommandMatch match = findTelegramCommand(text);

        if (match == null) {
            if (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("/help")) {
                sendTelegramMessageAsync(message.chatId(), telegramMessage("telegram.messages.usage", "Write: /tg <your_nick>", null));
                return true;
            }
            return false;
        }

        String playerName = firstWord(match.arguments());
        if (playerName.isEmpty()) {
            sendTelegramMessageAsync(message.chatId(), telegramMessage("telegram.messages.usage", "Write: /tg <your_nick>", null));
            return true;
        }

        if (!PLAYER_NAME.matcher(playerName).matches()) {
            sendTelegramMessageAsync(message.chatId(), telegramMessage("telegram.messages.invalid-player", "Invalid Minecraft nickname.", playerName));
            return true;
        }

        RewardResult result = grantReward(playerName, message.chatId(), message.fromId(), message.username());
        switch (result) {
            case SUCCESS -> {
                sendTelegramMessageAsync(message.chatId(), telegramListMessage("tg_message_player.linkAccount", playerName));
                sendAdminNotification(playerName, message.fromId(), message.username());
            }
            case ALREADY_PLAYER -> sendTelegramMessageAsync(message.chatId(), telegramMessage("telegram.messages.already-player", "This player already received the reward.", playerName));
            case OTHER_ACCOUNT -> sendTelegramMessageAsync(message.chatId(), telegramMessage("telegram.messages.other-account", "This Telegram account already received the reward for another player.", playerName));
        }
        return true;
    }

    private RewardResult grantReward(String playerName, long chatId, long telegramId, String telegramUsername) {
        String playerKey = playerName.toLowerCase(Locale.ROOT);
        String telegramKey = Long.toString(telegramId);

        synchronized (dataLock) {
            if (playerData.isConfigurationSection("players." + playerKey)) {
                return RewardResult.ALREADY_PLAYER;
            }

            String linkedPlayer = playerData.getString("telegram-users." + telegramKey + ".player", "");
            if (!linkedPlayer.isEmpty() && !linkedPlayer.equalsIgnoreCase(playerKey)) {
                return RewardResult.OTHER_ACCOUNT;
            }

            long now = Instant.now().toEpochMilli();
            playerData.set("players." + playerKey + ".name", playerName);
            playerData.set("players." + playerKey + ".telegram-id", telegramId);
            playerData.set("players." + playerKey + ".telegram-username", telegramUsername);
            playerData.set("players." + playerKey + ".chat-id", chatId);
            playerData.set("players." + playerKey + ".rewarded-at", now);
            playerData.set("telegram-users." + telegramKey + ".player", playerKey);
            playerData.set("telegram-users." + telegramKey + ".username", telegramUsername);
            playerData.set("telegram-users." + telegramKey + ".chat-id", chatId);
            playerData.set("telegram-users." + telegramKey + ".rewarded-at", now);
            savePlayerDataLocked();
        }

        runRewardCommands(playerName);

        Player onlinePlayer = findOnlinePlayer(playerName);
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(color(message("messages.successfulLinkTG", "&aReward received.", playerName)));
        }

        getLogger().info("Telegram social reward issued to " + playerName + " from Telegram ID " + telegramId + ".");
        return RewardResult.SUCCESS;
    }

    private void runRewardCommands(String playerName) {
        for (String command : getConfig().getStringList("tgCommands")) {
            String prepared = command
                    .replace("{player}", playerName)
                    .replace("%player%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
        }
    }

    private Player findOnlinePlayer(String playerName) {
        Player exact = Bukkit.getPlayerExact(playerName);
        if (exact != null) {
            return exact;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player;
            }
        }
        return null;
    }

    private boolean hasRewardForPlayer(String playerName) {
        synchronized (dataLock) {
            return playerData.isConfigurationSection("players." + playerName.toLowerCase(Locale.ROOT));
        }
    }

    private void sendAdminNotification(String playerName, long telegramId, String username) {
        String chatId = getConfig().getString("tg_settings.chat_id", "").trim();
        if (chatId.isEmpty()) {
            return;
        }

        String suffix = "\nTelegram ID: " + telegramId;
        if (username != null && !username.isBlank()) {
            suffix += "\nUsername: @" + username;
        }
        sendTelegramMessageAsync(chatId, telegramListMessage("tg_message_admin.linkAccount", playerName) + suffix);
    }

    private CommandMatch findTelegramCommand(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String command : telegramCommandAliases()) {
            String lowerCommand = command.toLowerCase(Locale.ROOT);
            if (lowerText.equals(lowerCommand)) {
                return new CommandMatch("");
            }
            if (lowerText.startsWith(lowerCommand + " ")) {
                return new CommandMatch(text.substring(command.length()).trim());
            }
        }
        return null;
    }

    private List<String> telegramCommandAliases() {
        List<String> commands = new ArrayList<>(getConfig().getStringList("telegram.command-aliases"));
        if (commands.isEmpty()) {
            commands.addAll(getConfig().getStringList("tg_message_player.command"));
        }
        if (commands.isEmpty()) {
            String legacyCommands = getConfig().getString("tg_message_player.command", "");
            if (!legacyCommands.isBlank()) {
                for (String part : legacyCommands.split("\\R")) {
                    if (!part.isBlank()) {
                        commands.add(part.trim());
                    }
                }
            }
        }
        if (commands.isEmpty()) {
            commands.addAll(DEFAULT_TG_COMMANDS);
        }

        commands.removeIf(String::isBlank);
        commands.sort(Comparator.comparingInt(String::length).reversed());
        return commands;
    }

    private String stripBotMention(String text) {
        int spaceIndex = text.indexOf(' ');
        String first = spaceIndex >= 0 ? text.substring(0, spaceIndex) : text;
        String rest = spaceIndex >= 0 ? text.substring(spaceIndex) : "";
        int mentionIndex = first.indexOf('@');
        if (mentionIndex > 0) {
            first = first.substring(0, mentionIndex);
        }
        return first + rest;
    }

    private String firstWord(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex >= 0 ? trimmed.substring(0, spaceIndex) : trimmed;
    }

    private String message(String path, String fallback, String playerName) {
        String value = getConfig().getString(path, fallback);
        if (playerName != null) {
            value = value.replace("{player}", playerName).replace("%player%", playerName);
        }
        return value;
    }

    private String telegramMessage(String path, String fallback, String playerName) {
        return plain(message(path, fallback, playerName));
    }

    private String telegramListMessage(String path, String playerName) {
        List<String> lines = getConfig().getStringList(path);
        if (lines.isEmpty()) {
            return telegramMessage("telegram.messages.success", "Reward received.", playerName);
        }

        List<String> prepared = new ArrayList<>();
        for (String line : lines) {
            prepared.add(plain(messageLine(line, playerName)));
        }
        return String.join("\n", prepared);
    }

    private String messageLine(String value, String playerName) {
        if (playerName == null) {
            return value;
        }
        return value.replace("{player}", playerName).replace("%player%", playerName);
    }

    private String plain(String value) {
        return ChatColor.stripColor(color(value));
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void sendTelegramMessageAsync(long chatId, String text) {
        sendTelegramMessageAsync(Long.toString(chatId), text);
    }

    private void sendTelegramMessageAsync(String chatId, String text) {
        String token = getConfig().getString("tg_settings.token", "").trim();
        if (token.isEmpty() || chatId.isBlank() || text.isBlank() || telegramSender == null || telegramSender.isShutdown()) {
            return;
        }

        telegramSender.submit(() -> {
            try {
                sendTelegramMessage(token, chatId, text);
            } catch (IOException exception) {
                getLogger().log(Level.WARNING, "Could not send Telegram message.", exception);
            }
        });
    }

    private void sendTelegramMessage(String token, String chatId, String text) throws IOException {
        String body = "chat_id=" + encode(chatId) + "&text=" + encode(text);
        httpPost(apiUrl(token, "sendMessage"), body, 10000);
    }

    private void savePlayerDataLocked() {
        try {
            playerData.save(dataFile);
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save playerdata.yml.", exception);
        }
    }

    private String apiUrl(String token, String method) {
        return "https://api.telegram.org/bot" + token + "/" + method;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String httpGet(String url, int readTimeoutMillis) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(readTimeoutMillis);
        return readConnection(connection);
    }

    private static String httpPost(String url, String body, int readTimeoutMillis) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(body);
        }
        return readConnection(connection);
    }

    private static String readConnection(HttpURLConnection connection) throws IOException {
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String response = readFully(stream);
        if (code >= 400) {
            throw new IOException("HTTP " + code + ": " + response);
        }
        return response;
    }

    private static String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }

    private enum RewardResult {
        SUCCESS,
        ALREADY_PLAYER,
        OTHER_ACCOUNT
    }

    private record CommandMatch(String arguments) {
    }

    private record TelegramMessage(long chatId, long fromId, String username, String text) {
    }

    private record TelegramUpdate(long updateId, TelegramMessage message) {
    }

    private final class TelegramPoller {
        private final String token;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Thread thread;

        private TelegramPoller(String token) {
            this.token = token;
        }

        private void start() {
            if (!running.compareAndSet(false, true)) {
                return;
            }

            thread = new Thread(this::pollLoop, "EpicSocialBonus-TelegramPoller");
            thread.setDaemon(true);
            thread.start();
        }

        private void stop() {
            running.set(false);
            if (thread != null) {
                thread.interrupt();
            }
        }

        private void pollLoop() {
            int retryDelayMillis = Math.max(1000, getConfig().getInt("telegram.retry-delay-millis", 5000));
            int timeoutSeconds = Math.max(1, getConfig().getInt("telegram.timeout-seconds", 25));
            long offset = getLastUpdateId() + 1;

            while (running.get() && isEnabled()) {
                try {
                    List<TelegramUpdate> updates = fetchUpdates(offset, timeoutSeconds);
                    for (TelegramUpdate update : updates) {
                        if (!running.get() || !isEnabled()) {
                            return;
                        }

                        if (update.message() != null) {
                            processMessageOnMainThread(update.message());
                        }

                        offset = update.updateId() + 1;
                        setLastUpdateId(update.updateId());
                    }
                } catch (IOException exception) {
                    getLogger().warning("Telegram polling failed: " + exception.getMessage());
                    sleepQuietly(retryDelayMillis);
                } catch (RuntimeException exception) {
                    getLogger().log(Level.WARNING, "Unexpected Telegram polling error.", exception);
                    sleepQuietly(retryDelayMillis);
                }
            }
        }

        private long getLastUpdateId() {
            synchronized (dataLock) {
                return playerData.getLong("meta.last-update-id", 0L);
            }
        }

        private void setLastUpdateId(long updateId) {
            synchronized (dataLock) {
                playerData.set("meta.last-update-id", updateId);
                savePlayerDataLocked();
            }
        }

        private void processMessageOnMainThread(TelegramMessage message) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                Bukkit.getScheduler().runTask(EpicSocialBonus.this, () -> {
                    try {
                        handleTelegramMessage(message);
                    } catch (RuntimeException exception) {
                        getLogger().log(Level.WARNING, "Could not process Telegram message.", exception);
                    } finally {
                        future.complete(null);
                    }
                });
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception exception) {
                future.cancel(true);
                getLogger().log(Level.WARNING, "Telegram message processing timed out or failed.", exception);
            }
        }

        private List<TelegramUpdate> fetchUpdates(long offset, int timeoutSeconds) throws IOException {
            String url = apiUrl(token, "getUpdates")
                    + "?offset=" + offset
                    + "&timeout=" + timeoutSeconds
                    + "&allowed_updates=" + encode("[\"message\"]");
            String response = httpGet(url, (timeoutSeconds + 15) * 1000);
            Object parsed = Json.parse(response);
            Map<String, Object> root = asMap(parsed);

            if (!Boolean.TRUE.equals(root.get("ok"))) {
                throw new IOException(Objects.toString(root.get("description"), "Telegram API returned ok=false"));
            }

            List<TelegramUpdate> updates = new ArrayList<>();
            for (Object item : asList(root.get("result"))) {
                Map<String, Object> update = asMap(item);
                long updateId = asLong(update.get("update_id"), 0L);
                TelegramMessage message = parseMessage(update.get("message"));
                updates.add(new TelegramUpdate(updateId, message));
            }
            return updates;
        }

        private TelegramMessage parseMessage(Object object) {
            if (!(object instanceof Map<?, ?>)) {
                return null;
            }

            Map<String, Object> message = asMap(object);
            String text = asString(message.get("text"));
            if (text == null || text.isBlank()) {
                return null;
            }

            Map<String, Object> chat = asMap(message.get("chat"));
            Map<String, Object> from = asMap(message.get("from"));
            long chatId = asLong(chat.get("id"), 0L);
            long fromId = asLong(from.get("id"), chatId);
            String username = asString(from.get("username"));

            if (chatId == 0L || fromId == 0L) {
                return null;
            }

            return new TelegramMessage(chatId, fromId, username, text);
        }

        private void sleepQuietly(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object object) {
        if (object instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(Objects.toString(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object object) {
        if (object instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private static String asString(Object object) {
        return object == null ? null : Objects.toString(object);
    }

    private static long asLong(Object object, long fallback) {
        if (object instanceof Number number) {
            return number.longValue();
        }
        if (object instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static final class Json {
        private final String input;
        private int index;

        private Json(String input) {
            this.input = input;
        }

        private static Object parse(String input) throws IOException {
            Json json = new Json(input);
            Object value = json.readValue();
            json.skipWhitespace();
            if (!json.isEnd()) {
                throw new IOException("Unexpected JSON content at position " + json.index + ".");
            }
            return value;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            if (isEnd()) {
                throw new IOException("Unexpected end of JSON.");
            }

            char current = input.charAt(index);
            return switch (current) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() throws IOException {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                Object value = readValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> readArray() throws IOException {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return array;
            }

            while (true) {
                array.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return array;
                }
                expect(',');
            }
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!isEnd()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current != '\\') {
                    result.append(current);
                    continue;
                }

                if (isEnd()) {
                    throw new IOException("Unexpected end of JSON string escape.");
                }

                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(readUnicodeEscape());
                    default -> throw new IOException("Invalid JSON escape: \\" + escaped);
                }
            }
            throw new IOException("Unclosed JSON string.");
        }

        private char readUnicodeEscape() throws IOException {
            if (index + 4 > input.length()) {
                throw new IOException("Invalid unicode escape.");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid unicode escape: " + hex, exception);
            }
        }

        private Object readNumber() throws IOException {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (!isEnd() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (!isEnd() && input.charAt(index) == '.') {
                index++;
                while (!isEnd() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (!isEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (!isEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                while (!isEnd() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }

            if (start == index) {
                throw new IOException("Expected JSON value at position " + index + ".");
            }

            String raw = input.substring(start, index);
            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    return Double.parseDouble(raw);
                }
                return Long.parseLong(raw);
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid JSON number: " + raw, exception);
            }
        }

        private Object readLiteral(String literal, Object value) throws IOException {
            if (!input.startsWith(literal, index)) {
                throw new IOException("Expected " + literal + " at position " + index + ".");
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) throws IOException {
            if (isEnd() || input.charAt(index) != expected) {
                throw new IOException("Expected '" + expected + "' at position " + index + ".");
            }
            index++;
        }

        private boolean peek(char expected) {
            return !isEnd() && input.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean isEnd() {
            return index >= input.length();
        }
    }
}
