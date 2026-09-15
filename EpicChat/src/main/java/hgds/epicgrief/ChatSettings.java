package hgds.epicgrief;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChatSettings {
    private static final String DEFAULT_FORMAT = "{sender}: {message}";

    private final Map<String, ChatChannel> channels;
    private final List<ChatChannel> prefixedChannels;
    private final List<ChatChannel> commandChannels;
    private final ChatChannel defaultChannel;
    private final String privateMessageFormat;
    private final long playTimeSeconds;
    private final Set<String> playTimeBlockedCommands;
    private final double capsPercent;
    private final double spamPercent;
    private final long spamTimeMillis;
    private final Pattern allowedTextPattern;
    private final Set<String> allowedSymbols;
    private final List<String> blackList;
    private final Map<String, String> messages;

    private ChatSettings(
            Map<String, ChatChannel> channels,
            List<ChatChannel> prefixedChannels,
            List<ChatChannel> commandChannels,
            ChatChannel defaultChannel,
            String privateMessageFormat,
            long playTimeSeconds,
            Set<String> playTimeBlockedCommands,
            double capsPercent,
            double spamPercent,
            long spamTimeMillis,
            Pattern allowedTextPattern,
            Set<String> allowedSymbols,
            List<String> blackList,
            Map<String, String> messages
    ) {
        this.channels = channels;
        this.prefixedChannels = prefixedChannels;
        this.commandChannels = commandChannels;
        this.defaultChannel = defaultChannel;
        this.privateMessageFormat = privateMessageFormat;
        this.playTimeSeconds = playTimeSeconds;
        this.playTimeBlockedCommands = playTimeBlockedCommands;
        this.capsPercent = capsPercent;
        this.spamPercent = spamPercent;
        this.spamTimeMillis = spamTimeMillis;
        this.allowedTextPattern = allowedTextPattern;
        this.allowedSymbols = allowedSymbols;
        this.blackList = blackList;
        this.messages = messages;
    }

    public static ChatSettings from(FileConfiguration config, Logger logger) {
        Map<String, ChatChannel> channels = new LinkedHashMap<>();
        List<ChatChannel> prefixedChannels = new ArrayList<>();
        List<ChatChannel> commandChannels = new ArrayList<>();

        ConfigurationSection chats = config.getConfigurationSection("chats");
        if (chats != null) {
            for (String key : chats.getKeys(false)) {
                ConfigurationSection section = chats.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String normalizedName = key.toLowerCase(Locale.ROOT);
                ChatChannel channel = new ChatChannel(
                        normalizedName,
                        section.getBoolean("command", false),
                        section.getString("prefix"),
                        section.getString("format", DEFAULT_FORMAT),
                        section.getString("permission"),
                        section.getDouble("range", -1.0D)
                );
                channels.put(normalizedName, channel);
                if (channel.getPrefix() != null) {
                    prefixedChannels.add(channel);
                }
                if (channel.isCommand()) {
                    commandChannels.add(channel);
                }
            }
        }

        ChatChannel defaultChannel = channels.get("default");
        if (defaultChannel == null) {
            defaultChannel = channels.values().stream().findFirst().orElse(
                    new ChatChannel("default", false, null, DEFAULT_FORMAT, null, -1.0D)
            );
            channels.putIfAbsent(defaultChannel.getName(), defaultChannel);
        }

        prefixedChannels.sort((left, right) -> Integer.compare(
                right.getPrefix().length(),
                left.getPrefix().length()
        ));

        Pattern allowedTextPattern = compileAllowedText(config.getString("allowedText", ""), logger);
        Set<String> allowedSymbols = new HashSet<>(config.getStringList("allowedSymbols"));

        Set<String> blockedCommands = new HashSet<>();
        for (String command : config.getStringList("playTime.blockedCommands")) {
            String normalized = command.toLowerCase(Locale.ROOT).replaceFirst("^/", "").trim();
            if (!normalized.isEmpty()) {
                blockedCommands.add(normalized);
            }
        }

        Map<String, String> messages = new HashMap<>(defaultMessages());
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, messages.getOrDefault(key, "")));
            }
        }

        return new ChatSettings(
                Collections.unmodifiableMap(channels),
                Collections.unmodifiableList(prefixedChannels),
                Collections.unmodifiableList(commandChannels),
                defaultChannel,
                config.getString("message", "§7{sender} -> {to}: {message}"),
                Math.max(0L, config.getLong("playTime.time", 0L)),
                Collections.unmodifiableSet(blockedCommands),
                Math.max(0.0D, config.getDouble("caps", 0.0D)),
                Math.max(0.0D, config.getDouble("spam.percent", 0.0D)),
                Math.max(0L, Math.round(config.getDouble("spam.time", 0.0D) * 1000.0D)),
                allowedTextPattern,
                Collections.unmodifiableSet(allowedSymbols),
                Collections.unmodifiableList(config.getStringList("blackList")),
                Collections.unmodifiableMap(messages)
        );
    }

    public Map<String, ChatChannel> getChannels() {
        return channels;
    }

    public List<ChatChannel> getCommandChannels() {
        return commandChannels;
    }

    public ChatChannel getChannel(String name) {
        if (name == null) {
            return null;
        }
        return channels.get(name.toLowerCase(Locale.ROOT));
    }

    public String getPrivateMessageFormat() {
        return privateMessageFormat;
    }

    public long getPlayTimeSeconds() {
        return playTimeSeconds;
    }

    public Set<String> getPlayTimeBlockedCommands() {
        return playTimeBlockedCommands;
    }

    public double getCapsPercent() {
        return capsPercent;
    }

    public double getSpamPercent() {
        return spamPercent;
    }

    public long getSpamTimeMillis() {
        return spamTimeMillis;
    }

    public Pattern getAllowedTextPattern() {
        return allowedTextPattern;
    }

    public Set<String> getAllowedSymbols() {
        return allowedSymbols;
    }

    public List<String> getBlackList() {
        return blackList;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, messages.getOrDefault("NO_PERMISSION", "§cNo permission."));
    }

    public ChatSelection selectChannel(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage;
        for (ChatChannel channel : prefixedChannels) {
            if (message.startsWith(channel.getPrefix())) {
                return new ChatSelection(channel, message.substring(channel.getPrefix().length()).trim());
            }
        }
        return new ChatSelection(defaultChannel, message.trim());
    }

    private static Pattern compileAllowedText(String rawPattern, Logger logger) {
        if (rawPattern == null || rawPattern.trim().isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(rawPattern);
        } catch (PatternSyntaxException exception) {
            logger.warning("Invalid allowedText pattern, regex check disabled: " + exception.getMessage());
            return null;
        }
    }

    private static Map<String, String> defaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("SPAM", "§cDo not send messages so quickly.");
        defaults.put("PLAY_TIME", "§cChat will be available in %s.");
        defaults.put("CAPS", "§cDo not use caps.");
        defaults.put("UNALLOWED_SYMBOL", "§cDo not use forbidden symbols.");
        defaults.put("BLACKWORD", "§cThis message is blocked.");
        defaults.put("REPLY_USE", "§cUse /r <message>");
        defaults.put("MESSAGE_USE", "§cUse /msg <player> <message>");
        defaults.put("REPLY_NONE", "§cThere is nobody to reply to.");
        defaults.put("PLAYER_OFFLINE", "§cPlayer is offline.");
        defaults.put("MESSAGE_SELF", "§cYou cannot message yourself.");
        defaults.put("PLAY_ACTIONBAR", "§fChat will be available in §6%s§f.");
        defaults.put("PLAY_ACTIONBAR_FINISH", "§aChat is now available.");
        defaults.put("SPY_OFF", "§cSocial spy disabled.");
        defaults.put("SPY_ON", "§aSocial spy enabled.");
        defaults.put("SPY", "§7<{sender} -> {player}> {msg}");
        defaults.put("NO_PERMISSION", "§cYou do not have permission.");
        defaults.put("MESSAGE_DISABLED", "§cThis player has private messages disabled.");
        defaults.put("MESSAGE_OFF", "§cPrivate messages disabled.");
        defaults.put("MESSAGE_ON", "§aPrivate messages enabled.");
        return defaults;
    }

    public static final class ChatSelection {
        private final ChatChannel channel;
        private final String message;

        private ChatSelection(ChatChannel channel, String message) {
            this.channel = channel;
            this.message = message;
        }

        public ChatChannel getChannel() {
            return channel;
        }

        public String getMessage() {
            return message;
        }
    }
}
