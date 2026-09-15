package hgds.epicgrief;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class MessageGuard {
    private final EpicChat plugin;
    private final Map<UUID, LastMessage> lastMessages = new ConcurrentHashMap<>();

    public MessageGuard(EpicChat plugin) {
        this.plugin = plugin;
    }

    public GuardResult check(Player player, String message) {
        if (player.hasPermission("chat.bypass")) {
            return GuardResult.allowed();
        }

        ChatSettings settings = plugin.getSettings();
        if (!isAllowedText(settings, message)) {
            return GuardResult.denied("UNALLOWED_SYMBOL");
        }
        if (containsBlacklistedValue(settings, message)) {
            return GuardResult.denied("BLACKWORD");
        }
        if (isCaps(settings, message)) {
            return GuardResult.denied("CAPS");
        }
        if (isSpam(settings, player, message)) {
            return GuardResult.denied("SPAM");
        }
        return GuardResult.allowed();
    }

    public void remember(Player player, String message) {
        lastMessages.put(player.getUniqueId(), new LastMessage(normalizeForSpam(message), System.currentTimeMillis()));
    }

    private boolean isAllowedText(ChatSettings settings, String message) {
        Pattern pattern = settings.getAllowedTextPattern();
        if (pattern != null && pattern.matcher(message).matches()) {
            return true;
        }

        if (settings.getAllowedSymbols().isEmpty()) {
            return pattern == null;
        }

        for (int index = 0; index < message.length(); ) {
            int codePoint = message.codePointAt(index);
            String symbol = new String(Character.toChars(codePoint));
            if (!isBaseAllowedCodePoint(codePoint)
                    && !Character.isWhitespace(codePoint)
                    && !settings.getAllowedSymbols().contains(symbol)) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }

    private boolean isBaseAllowedCodePoint(int codePoint) {
        return Character.isDigit(codePoint)
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 0x0400 && codePoint <= 0x04FF);
    }

    private boolean containsBlacklistedValue(ChatSettings settings, String message) {
        if (settings.getBlackList().isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        String compact = lower.replaceAll("\\s+", "");
        for (String rawValue : settings.getBlackList()) {
            String value = rawValue == null ? "" : rawValue.toLowerCase(Locale.ROOT).trim();
            if (value.isEmpty()) {
                continue;
            }
            if (compact.contains("." + value)
                    || compact.contains("," + value)
                    || compact.contains("/" + value)
                    || containsToken(lower, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsToken(String text, String token) {
        Pattern tokenPattern = Pattern.compile(
                "(^|[^\\p{L}\\p{N}])" + Pattern.quote(token) + "($|[^\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        return tokenPattern.matcher(text).find();
    }

    private boolean isCaps(ChatSettings settings, String message) {
        double limit = settings.getCapsPercent();
        if (limit <= 0.0D) {
            return false;
        }

        int letters = 0;
        int uppercase = 0;
        for (int index = 0; index < message.length(); ) {
            int codePoint = message.codePointAt(index);
            if (Character.isLetter(codePoint)) {
                letters++;
                if (Character.isUpperCase(codePoint)) {
                    uppercase++;
                }
            }
            index += Character.charCount(codePoint);
        }

        return letters >= 5 && uppercase * 100.0D / letters >= limit;
    }

    private boolean isSpam(ChatSettings settings, Player player, String message) {
        if (settings.getSpamTimeMillis() <= 0L) {
            return false;
        }

        LastMessage lastMessage = lastMessages.get(player.getUniqueId());
        if (lastMessage == null) {
            return false;
        }
        long age = System.currentTimeMillis() - lastMessage.timeMillis;
        if (age >= settings.getSpamTimeMillis()) {
            return false;
        }

        double similarity = similarityPercent(normalizeForSpam(message), lastMessage.normalizedMessage);
        return similarity >= settings.getSpamPercent();
    }

    private String normalizeForSpam(String message) {
        String normalized = message.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        if (normalized.length() > 512) {
            return normalized.substring(0, 512);
        }
        return normalized;
    }

    private double similarityPercent(String left, String right) {
        if (left.equals(right)) {
            return 100.0D;
        }
        int max = Math.max(left.length(), right.length());
        if (max == 0) {
            return 100.0D;
        }
        int distance = levenshteinDistance(left, right);
        return Math.max(0.0D, (1.0D - distance / (double) max) * 100.0D);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }

        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int cost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private static final class LastMessage {
        private final String normalizedMessage;
        private final long timeMillis;

        private LastMessage(String normalizedMessage, long timeMillis) {
            this.normalizedMessage = normalizedMessage;
            this.timeMillis = timeMillis;
        }
    }
}
