package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class EpicChat extends JavaPlugin {
    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
    private final Map<String, Command> dynamicChannelCommands = new HashMap<>();
    private final List<String> dynamicCommandLabels = new ArrayList<>();
    private final java.util.Set<UUID> disabledPrivateMessages = ConcurrentHashMap.newKeySet();
    private final java.util.Set<UUID> socialSpyPlayers = ConcurrentHashMap.newKeySet();

    private ChatSettings settings;
    private MessageGuard messageGuard;
    private TextFormatter formatter;
    private BukkitTask playTimeTask;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();

            formatter = new TextFormatter(this);
            messageGuard = new MessageGuard(this);
            reloadSettings();

            registerBaseCommands();
            registerConfiguredChannelCommands();
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            restartPlayTimeNotifier();

            getLogger().info("Loaded " + settings.getChannels().size() + " chat channels.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "EpicChat could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (playTimeTask != null) {
            playTimeTask.cancel();
            playTimeTask = null;
        }
        unregisterDynamicChannelCommands();
        socialSpyPlayers.clear();
        disabledPrivateMessages.clear();
        replyTargets.clear();
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadSettings();
        registerConfiguredChannelCommands();
        restartPlayTimeNotifier();
    }

    public ChatSettings getSettings() {
        return settings;
    }

    public TextFormatter getFormatter() {
        return formatter;
    }

    public boolean handlePublicChat(Player player, String rawMessage) {
        ChatSettings.ChatSelection selection = settings.selectChannel(rawMessage);
        return sendChannelMessage(player, selection.getChannel(), selection.getMessage());
    }

    public boolean handleConfiguredChannelCommand(Player player, String channelName, String rawMessage) {
        ChatChannel channel = settings.getChannel(channelName);
        if (channel == null) {
            sendConfiguredMessage(player, "NO_PERMISSION");
            return true;
        }
        return sendChannelMessage(player, channel, rawMessage);
    }

    public boolean sendPrivateMessage(Player sender, Player target, String rawMessage) {
        if (sender.equals(target)) {
            sendConfiguredMessage(sender, "MESSAGE_SELF");
            return false;
        }
        if (isPrivateMessagesDisabled(target) && !sender.hasPermission("chat.bypass")) {
            sendConfiguredMessage(sender, "MESSAGE_DISABLED");
            return false;
        }

        String preparedMessage = prepareOutgoingMessage(sender, rawMessage);
        if (preparedMessage == null) {
            return false;
        }

        String formatted = formatter.formatPrivateMessage(sender, target, preparedMessage);
        sender.sendMessage(formatted);
        target.sendMessage(formatted);
        replyTargets.put(sender.getUniqueId(), target.getUniqueId());
        replyTargets.put(target.getUniqueId(), sender.getUniqueId());
        sendSpyMessage(sender, target, preparedMessage);
        return true;
    }

    public Player getReplyTarget(Player player) {
        UUID targetId = replyTargets.get(player.getUniqueId());
        if (targetId == null) {
            return null;
        }
        return Bukkit.getPlayer(targetId);
    }

    public boolean isPrivateMessagesDisabled(Player player) {
        return disabledPrivateMessages.contains(player.getUniqueId());
    }

    public boolean togglePrivateMessages(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPrivateMessages.remove(uuid)) {
            return true;
        }
        disabledPrivateMessages.add(uuid);
        return false;
    }

    public boolean toggleSocialSpy(Player player) {
        UUID uuid = player.getUniqueId();
        if (socialSpyPlayers.remove(uuid)) {
            return false;
        }
        socialSpyPlayers.add(uuid);
        return true;
    }

    public boolean isPlayTimeReady(Player player) {
        long requiredSeconds = settings.getPlayTimeSeconds();
        return requiredSeconds <= 0 || getPlayedSeconds(player) >= requiredSeconds;
    }

    public boolean isBlockedByPlayTimeCommand(String label) {
        return !settings.getPlayTimeBlockedCommands().isEmpty()
                && settings.getPlayTimeBlockedCommands().contains(label.toLowerCase(Locale.ROOT));
    }

    public void sendPlayTimeBlocked(Player player) {
        long remaining = Math.max(0L, settings.getPlayTimeSeconds() - getPlayedSeconds(player));
        sendConfiguredMessage(player, "PLAY_TIME", formatDuration(remaining));
    }

    public void sendConfiguredMessage(CommandSender sender, String key, Object... args) {
        String template = settings.getMessage(key);
        String message = template;
        if (args.length > 0) {
            try {
                message = String.format(template, args);
            } catch (RuntimeException ignored) {
                message = template;
            }
        }
        message = formatter.color(message);
        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.stripColor(message));
        }
    }

    private void reloadSettings() {
        settings = ChatSettings.from(getConfig(), getLogger());
        formatter.refreshHooks();
    }

    private boolean sendChannelMessage(Player player, ChatChannel channel, String rawMessage) {
        if (channel == null) {
            return true;
        }
        if (!channel.canUse(player)) {
            sendConfiguredMessage(player, channel.getName() + "_NO_PERM");
            return true;
        }

        String preparedMessage = prepareOutgoingMessage(player, rawMessage);
        if (preparedMessage == null) {
            return true;
        }

        String formatted = formatter.formatChannelMessage(channel, player, preparedMessage);
        int recipients = 0;
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!channel.canReceive(player, recipient)) {
                continue;
            }
            recipient.sendMessage(formatted);
            recipients++;
        }
        if (recipients == 0) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(formatted));
        return true;
    }

    private String prepareOutgoingMessage(Player player, String rawMessage) {
        if (!isPlayTimeReady(player)) {
            sendPlayTimeBlocked(player);
            return null;
        }

        String preparedMessage = formatter.preparePlayerMessage(player, rawMessage);
        String plainMessage = formatter.stripColors(preparedMessage).trim();
        if (plainMessage.isEmpty()) {
            return null;
        }

        GuardResult guardResult = messageGuard.check(player, plainMessage);
        if (!guardResult.isAllowed()) {
            sendConfiguredMessage(player, guardResult.getMessageKey());
            return null;
        }

        messageGuard.remember(player, plainMessage);
        return preparedMessage.trim();
    }

    private void sendSpyMessage(Player sender, Player target, String message) {
        String formatted = formatter.formatSpyMessage(sender, target, message);
        for (Player spy : Bukkit.getOnlinePlayers()) {
            if (spy.equals(sender) || spy.equals(target)) {
                continue;
            }
            if (socialSpyPlayers.contains(spy.getUniqueId()) && spy.hasPermission("chat.spy")) {
                spy.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(formatted));
    }

    private void registerBaseCommands() {
        PrivateMessageCommand privateMessageCommand = new PrivateMessageCommand(this);
        registerCommand("msg", privateMessageCommand, privateMessageCommand);

        ReplyCommand replyCommand = new ReplyCommand(this);
        registerCommand("reply", replyCommand, replyCommand);

        MessageToggleCommand toggleCommand = new MessageToggleCommand(this);
        registerCommand("msgtoggle", toggleCommand, toggleCommand);

        SocialSpyCommand spyCommand = new SocialSpyCommand(this);
        registerCommand("socialspy", spyCommand, spyCommand);

        EpicChatCommand epicChatCommand = new EpicChatCommand(this);
        registerCommand("epicchat", epicChatCommand, epicChatCommand);
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }

    private void registerConfiguredChannelCommands() {
        unregisterDynamicChannelCommands();

        for (ChatChannel channel : settings.getCommandChannels()) {
            String commandName = channel.getName().toLowerCase(Locale.ROOT);
            ConfiguredChannelExecutor executor = new ConfiguredChannelExecutor(this, commandName);

            PluginCommand pluginCommand = getCommand(commandName);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(executor);
                pluginCommand.setTabCompleter(executor);
                continue;
            }

            registerDynamicChannelCommand(commandName, executor);
        }
    }

    private void registerDynamicChannelCommand(String commandName, ConfiguredChannelExecutor executor) {
        try {
            CommandMap commandMap = getCommandMap();
            ConfiguredChannelCommand command = new ConfiguredChannelCommand(commandName, executor);
            ChatChannel channel = settings.getChannel(commandName);
            if (channel != null && channel.getPermission() != null) {
                command.setPermission(channel.getPermission());
            }
            commandMap.register(getName().toLowerCase(Locale.ROOT), command);
            dynamicChannelCommands.put(commandName, command);
            dynamicCommandLabels.add(commandName);
            dynamicCommandLabels.add(getName().toLowerCase(Locale.ROOT) + ":" + commandName);
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Could not register chat command '/" + commandName + "'", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void unregisterDynamicChannelCommands() {
        if (dynamicChannelCommands.isEmpty()) {
            return;
        }
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap instanceof SimpleCommandMap) {
                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
                for (String label : dynamicCommandLabels) {
                    knownCommands.remove(label);
                }
            }
            for (Command command : dynamicChannelCommands.values()) {
                command.unregister(commandMap);
            }
        } catch (ReflectiveOperationException exception) {
            getLogger().log(Level.WARNING, "Could not unregister previous chat commands", exception);
        } finally {
            dynamicChannelCommands.clear();
            dynamicCommandLabels.clear();
        }
    }

    private CommandMap getCommandMap() throws ReflectiveOperationException {
        Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        return (CommandMap) commandMapField.get(Bukkit.getServer());
    }

    private void restartPlayTimeNotifier() {
        if (playTimeTask != null) {
            playTimeTask.cancel();
            playTimeTask = null;
        }
        if (settings.getPlayTimeSeconds() <= 0) {
            return;
        }

        java.util.Set<UUID> notified = ConcurrentHashMap.newKeySet();
        playTimeTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                long remaining = settings.getPlayTimeSeconds() - getPlayedSeconds(player);
                if (remaining > 0) {
                    notified.remove(player.getUniqueId());
                    formatter.sendActionBar(player, String.format(settings.getMessage("PLAY_ACTIONBAR"), formatDuration(remaining)));
                    continue;
                }
                if (notified.add(player.getUniqueId())) {
                    formatter.sendActionBar(player, settings.getMessage("PLAY_ACTIONBAR_FINISH"));
                }
            }
        }, 20L, 20L);
    }

    private long getPlayedSeconds(Player player) {
        try {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        } catch (RuntimeException exception) {
            return Long.MAX_VALUE;
        }
    }

    private String formatDuration(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long restSeconds = seconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + restSeconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + restSeconds + "s";
        }
        return restSeconds + "s";
    }
}
