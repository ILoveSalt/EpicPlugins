package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EpicTG extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private static final Pattern MINECRAFT_NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final String TOKEN_PLACEHOLDER = "PASTE_TELEGRAM_BOT_TOKEN_HERE";

    private final SecureRandom random = new SecureRandom();
    private File playerDataFile;
    private FileConfiguration playerData;
    private TelegramBotService botService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPlayerData();

        registerCommand("confirm");
        registerCommand("epictg");
        getServer().getPluginManager().registerEvents(this, this);
        startTelegramBot();
    }

    @Override
    public void onDisable() {
        if (botService != null) {
            botService.stop();
            botService = null;
        }
        savePlayerData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("confirm")) {
            return handleConfirmCommand(sender, args);
        }

        if (command.getName().equalsIgnoreCase("epictg")) {
            return handleAdminCommand(sender, args);
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("epictg") && args.length == 1 && sender.hasPermission("epictg.admin")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PendingLink pendingLink = findPendingByNick(event.getPlayer().getName());
        if (pendingLink == null) {
            return;
        }

        event.getPlayer().sendMessage(color(getConfig().getString("messages.game.pending-code", "&6EpicTG &8» &fДля привязки Telegram напишите &e/confirm %code%")
                .replace("%code%", pendingLink.code)));
    }

    void handleTelegramMessage(long chatId, long telegramId, String username, String firstName, String text) {
        String message = text.trim();
        if (message.isEmpty()) {
            return;
        }

        if (isStartCommand(message)) {
            showStart(chatId, telegramId);
            return;
        }

        if (isLinkCommand(message)) {
            handleLinkCommand(chatId, telegramId, username, message);
            return;
        }

        if (!isTelegramLinked(telegramId)) {
            sendTelegram(chatId, telegramMessage("telegram.messages.need-link"), null);
            return;
        }

        if (equalsButton(message, "shop")) {
            sendTelegram(chatId, telegramMessage("telegram.messages.shop-stub"), mainMenuMarkup());
            return;
        }

        if (equalsButton(message, "profile")) {
            sendProfile(chatId, telegramId);
            return;
        }

        if (equalsButton(message, "support")) {
            sendTelegram(chatId, telegramMessage("telegram.messages.support-stub"), mainMenuMarkup());
            return;
        }

        if (equalsButton(message, "reward")) {
            claimReward(chatId, telegramId);
            return;
        }

        sendTelegram(chatId, telegramMessage("telegram.messages.unknown"), mainMenuMarkup());
    }

    private boolean handleConfirmCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(getConfig().getString("messages.game.only-players", "&cКоманда доступна только игрокам.")));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(color(getConfig().getString("messages.game.confirm-usage", "&fИспользование: &e/confirm <код>")));
            return true;
        }

        Player player = (Player) sender;
        String code = stripQuotes(args[0]);
        PendingLink pendingLink = findPendingByCode(code);
        if (pendingLink == null) {
            player.sendMessage(color(getConfig().getString("messages.game.code-not-found", "&cКод не найден или уже истек.")));
            return true;
        }

        if (!pendingLink.nick.equalsIgnoreCase(player.getName())) {
            player.sendMessage(color(getConfig().getString("messages.game.wrong-player", "&cЭтот код создан для ника &e%player%&c.")
                    .replace("%player%", pendingLink.nick)));
            return true;
        }

        String linkedTelegram = playerData.getString("nicks." + normalizeNick(player.getName()));
        if (linkedTelegram != null && !linkedTelegram.equals(String.valueOf(pendingLink.telegramId))) {
            player.sendMessage(color(getConfig().getString("messages.game.nick-already-linked", "&cЭтот ник уже привязан к другому Telegram.")));
            return true;
        }

        String linkedNick = getLinkedNick(pendingLink.telegramId);
        if (linkedNick != null && !linkedNick.equalsIgnoreCase(player.getName())) {
            player.sendMessage(color(getConfig().getString("messages.game.telegram-already-linked", "&cЭтот Telegram уже привязан к другому нику.")));
            return true;
        }

        linkAccount(pendingLink, player.getName());
        removePendingCode(code);
        savePlayerData();

        player.sendMessage(color(getConfig().getString("messages.game.link-success", "&aАккаунт успешно привязан к Telegram.")));
        sendTelegram(pendingLink.chatId, telegramMessage("telegram.messages.link-success").replace("%player%", player.getName()), mainMenuMarkup());
        return true;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("epictg.admin")) {
            sender.sendMessage(color(getConfig().getString("messages.game.no-permission", "&cНет прав.")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadPlayerData();
            restartTelegramBot();
            sender.sendMessage(color(getConfig().getString("messages.game.reload", "&aEpicTG перезагружен.")));
            return true;
        }

        sender.sendMessage(color("&fИспользование: &e/epictg reload"));
        return true;
    }

    private void handleLinkCommand(long chatId, long telegramId, String username, String text) {
        String nick = parseLinkNick(text);
        if (nick == null || nick.isEmpty()) {
            sendTelegram(chatId, telegramMessage("telegram.messages.link-usage"), null);
            return;
        }

        if (!MINECRAFT_NICK.matcher(nick).matches()) {
            sendTelegram(chatId, telegramMessage("telegram.messages.invalid-nick"), null);
            return;
        }

        String existingNick = getLinkedNick(telegramId);
        if (existingNick != null) {
            sendTelegram(chatId, telegramMessage("telegram.messages.already-linked").replace("%player%", existingNick), mainMenuMarkup());
            return;
        }

        String linkedTelegram = playerData.getString("nicks." + normalizeNick(nick));
        if (linkedTelegram != null && !linkedTelegram.equals(String.valueOf(telegramId))) {
            sendTelegram(chatId, telegramMessage("telegram.messages.nick-busy"), null);
            return;
        }

        removePendingFor(telegramId, nick);

        String code = generateCode();
        String path = "pending." + code;
        playerData.set(path + ".telegram-id", telegramId);
        playerData.set(path + ".chat-id", chatId);
        playerData.set(path + ".nick", nick);
        playerData.set(path + ".username", username == null ? "" : username);
        playerData.set(path + ".created-at", System.currentTimeMillis());
        savePlayerData();

        String textToTelegram = telegramMessage("telegram.messages.code-created")
                .replace("%player%", nick)
                .replace("%code%", code)
                .replace("%minutes%", String.valueOf(getCodeExpireMinutes()));
        sendTelegram(chatId, textToTelegram, null);

        Player player = findOnlinePlayer(nick);
        if (player != null) {
            player.sendMessage(color(getConfig().getString("messages.game.pending-code", "&6EpicTG &8» &fДля привязки Telegram напишите &e/confirm %code%")
                    .replace("%code%", code)));
        }
    }

    private void showStart(long chatId, long telegramId) {
        String nick = getLinkedNick(telegramId);
        if (nick == null) {
            sendTelegram(chatId, telegramMessage("telegram.messages.start"), null);
            return;
        }

        sendTelegram(chatId, telegramMessage("telegram.messages.menu").replace("%player%", nick), mainMenuMarkup());
    }

    private void sendProfile(long chatId, long telegramId) {
        String path = playerPath(telegramId);
        String nick = playerData.getString(path + ".nick", "unknown");
        boolean rewardTaken = playerData.getBoolean(path + ".reward-taken", false);
        String rewardStatus = rewardTaken
                ? telegramMessage("telegram.messages.reward-status-taken")
                : telegramMessage("telegram.messages.reward-status-available");

        sendTelegram(chatId, telegramMessage("telegram.messages.profile")
                .replace("%player%", nick)
                .replace("%reward_status%", rewardStatus), mainMenuMarkup());
    }

    private void claimReward(long chatId, long telegramId) {
        String path = playerPath(telegramId);
        String nick = playerData.getString(path + ".nick");
        if (nick == null) {
            sendTelegram(chatId, telegramMessage("telegram.messages.need-link"), null);
            return;
        }

        if (playerData.getBoolean(path + ".reward-taken", false)) {
            sendTelegram(chatId, telegramMessage("telegram.messages.reward-already-taken"), mainMenuMarkup());
            return;
        }

        List<String> commands = getRewardCommands();
        playerData.set(path + ".reward-taken", true);
        savePlayerData();

        boolean dispatched = true;
        for (String command : commands) {
            String readyCommand = command.replace("%player%", nick);
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), readyCommand)) {
                dispatched = false;
            }
        }

        if (!dispatched) {
            playerData.set(path + ".reward-taken", false);
            savePlayerData();
            sendTelegram(chatId, telegramMessage("telegram.messages.reward-failed"), mainMenuMarkup());
            return;
        }

        sendTelegram(chatId, telegramMessage("telegram.messages.reward-success"), mainMenuMarkup());
        Player player = findOnlinePlayer(nick);
        if (player != null) {
            player.sendMessage(color(getConfig().getString("messages.game.reward-success", "&aВы получили награду за привязку Telegram.")));
        }
    }

    private void loadPlayerData() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            saveResource("playerdata.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        cleanupExpiredCodes();
    }

    private void savePlayerData() {
        if (playerData == null || playerDataFile == null) {
            return;
        }

        try {
            playerData.save(playerDataFile);
        } catch (IOException exception) {
            getLogger().warning("Could not save playerdata.yml: " + exception.getMessage());
        }
    }

    private void startTelegramBot() {
        if (!getConfig().getBoolean("telegram.enabled", false)) {
            getLogger().info("Telegram bot is disabled in config.yml.");
            return;
        }

        String token = getConfig().getString("telegram.token", "").trim();
        if (token.isEmpty() || token.equals(TOKEN_PLACEHOLDER)) {
            getLogger().warning("Telegram bot token is empty. Set telegram.token in config.yml.");
            return;
        }

        int timeoutSeconds = Math.max(5, getConfig().getInt("telegram.poll-timeout-seconds", 25));
        botService = new TelegramBotService(this, token, timeoutSeconds, this::handleTelegramMessage);
        botService.start();
    }

    private void restartTelegramBot() {
        if (botService != null) {
            botService.stop();
            botService = null;
        }
        startTelegramBot();
    }

    private void registerCommand(String commandName) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command /" + commandName + " is not registered in plugin.yml.");
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void linkAccount(PendingLink pendingLink, String confirmedNick) {
        String path = playerPath(pendingLink.telegramId);
        playerData.set(path + ".nick", confirmedNick);
        playerData.set(path + ".username", pendingLink.username);
        playerData.set(path + ".chat-id", pendingLink.chatId);
        playerData.set(path + ".linked-at", System.currentTimeMillis());
        if (!playerData.contains(path + ".reward-taken")) {
            playerData.set(path + ".reward-taken", false);
        }
        playerData.set("nicks." + normalizeNick(confirmedNick), String.valueOf(pendingLink.telegramId));
    }

    private PendingLink findPendingByCode(String code) {
        cleanupExpiredCodes();
        ConfigurationSection section = playerData.getConfigurationSection("pending." + code);
        if (section == null) {
            return null;
        }
        return readPendingLink(code, section);
    }

    private PendingLink findPendingByNick(String nick) {
        cleanupExpiredCodes();
        ConfigurationSection section = playerData.getConfigurationSection("pending");
        if (section == null) {
            return null;
        }

        for (String code : section.getKeys(false)) {
            ConfigurationSection pendingSection = section.getConfigurationSection(code);
            if (pendingSection == null) {
                continue;
            }

            PendingLink pendingLink = readPendingLink(code, pendingSection);
            if (pendingLink.nick.equalsIgnoreCase(nick)) {
                return pendingLink;
            }
        }
        return null;
    }

    private PendingLink readPendingLink(String code, ConfigurationSection section) {
        return new PendingLink(
                code,
                section.getLong("telegram-id"),
                section.getLong("chat-id"),
                section.getString("nick", ""),
                section.getString("username", ""),
                section.getLong("created-at")
        );
    }

    private void cleanupExpiredCodes() {
        if (playerData == null) {
            return;
        }

        ConfigurationSection section = playerData.getConfigurationSection("pending");
        if (section == null) {
            return;
        }

        boolean changed = false;
        for (String code : new ArrayList<>(section.getKeys(false))) {
            ConfigurationSection pendingSection = section.getConfigurationSection(code);
            if (pendingSection == null) {
                continue;
            }

            if (isExpired(pendingSection.getLong("created-at"))) {
                playerData.set("pending." + code, null);
                changed = true;
            }
        }

        if (changed) {
            savePlayerData();
        }
    }

    private boolean isExpired(long createdAt) {
        long expireMillis = Math.max(1L, getCodeExpireMinutes()) * 60_000L;
        return System.currentTimeMillis() - createdAt > expireMillis;
    }

    private void removePendingCode(String code) {
        playerData.set("pending." + code, null);
    }

    private void removePendingFor(long telegramId, String nick) {
        ConfigurationSection section = playerData.getConfigurationSection("pending");
        if (section == null) {
            return;
        }

        boolean changed = false;
        String normalizedNick = normalizeNick(nick);
        for (String code : new ArrayList<>(section.getKeys(false))) {
            ConfigurationSection pendingSection = section.getConfigurationSection(code);
            if (pendingSection == null) {
                continue;
            }

            long storedTelegramId = pendingSection.getLong("telegram-id");
            String storedNick = normalizeNick(pendingSection.getString("nick", ""));
            if (storedTelegramId == telegramId || storedNick.equals(normalizedNick)) {
                playerData.set("pending." + code, null);
                changed = true;
            }
        }

        if (changed) {
            savePlayerData();
        }
    }

    private String generateCode() {
        String code;
        do {
            code = String.valueOf(100000 + random.nextInt(900000));
        } while (playerData.contains("pending." + code));
        return code;
    }

    private boolean isTelegramLinked(long telegramId) {
        return getLinkedNick(telegramId) != null;
    }

    private String getLinkedNick(long telegramId) {
        return playerData.getString(playerPath(telegramId) + ".nick");
    }

    private String playerPath(long telegramId) {
        return "players." + telegramId;
    }

    private String normalizeNick(String nick) {
        return nick.toLowerCase(Locale.ROOT);
    }

    private int getCodeExpireMinutes() {
        return Math.max(1, getConfig().getInt("telegram.link-code-expire-minutes", 10));
    }

    private List<String> getRewardCommands() {
        List<String> commands = getConfig().getStringList("reward.commands");
        if (commands.isEmpty()) {
            commands = getConfig().getStringList("reward-commands");
        }
        if (commands.isEmpty()) {
            commands = Collections.singletonList("coins add %player% 4000");
        }
        return commands;
    }

    private Player findOnlinePlayer(String nick) {
        Player exact = Bukkit.getPlayerExact(nick);
        if (exact != null) {
            return exact;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(nick)) {
                return player;
            }
        }
        return null;
    }

    private boolean isStartCommand(String text) {
        String command = normalizeTelegramCommand(text);
        return command.equals("start") || command.equals("menu");
    }

    private boolean isLinkCommand(String text) {
        String prepared = text.trim();
        if (prepared.startsWith("-")) {
            prepared = prepared.substring(1).trim();
        }
        String command = normalizeTelegramCommand(prepared);
        return command.equals("link");
    }

    private String parseLinkNick(String text) {
        String prepared = text.trim();
        if (prepared.startsWith("-")) {
            prepared = prepared.substring(1).trim();
        }

        int firstSpace = prepared.indexOf(' ');
        if (firstSpace < 0) {
            return "";
        }
        return stripQuotes(prepared.substring(firstSpace + 1).trim());
    }

    private String normalizeTelegramCommand(String text) {
        String firstPart = text.trim().split("\\s+", 2)[0];
        if (firstPart.startsWith("/")) {
            firstPart = firstPart.substring(1);
        }

        int botMentionIndex = firstPart.indexOf('@');
        if (botMentionIndex >= 0) {
            firstPart = firstPart.substring(0, botMentionIndex);
        }

        return firstPart.toLowerCase(Locale.ROOT);
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean equalsButton(String text, String buttonKey) {
        return text.equalsIgnoreCase(getConfig().getString("telegram.buttons." + buttonKey, buttonKey));
    }

    private void sendTelegram(long chatId, String text, Object replyMarkup) {
        if (botService == null) {
            return;
        }
        botService.sendMessage(chatId, text, replyMarkup);
    }

    private Object mainMenuMarkup() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(getConfig().getString("telegram.buttons.shop", "Shop")));
        rows.add(Collections.singletonList(getConfig().getString("telegram.buttons.profile", "Profile")));
        rows.add(Collections.singletonList(getConfig().getString("telegram.buttons.support", "Support")));
        rows.add(Collections.singletonList(getConfig().getString("telegram.buttons.reward", "Reward")));
        return TelegramBotService.replyKeyboard(rows);
    }

    private String telegramMessage(String path) {
        return getConfig().getString(path, path).replace("\\n", "\n");
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private static final class PendingLink {
        private final String code;
        private final long telegramId;
        private final long chatId;
        private final String nick;
        private final String username;
        private final long createdAt;

        private PendingLink(String code, long telegramId, long chatId, String nick, String username, long createdAt) {
            this.code = code;
            this.telegramId = telegramId;
            this.chatId = chatId;
            this.nick = nick;
            this.username = username;
            this.createdAt = createdAt;
        }
    }
}
