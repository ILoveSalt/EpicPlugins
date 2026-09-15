package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ActionExecutor {
    private static final Pattern INTEGER_OPTION = Pattern.compile(
            "(?i)(?:^|\\s)(?:--?)?%s(?:\\s+|=)(-?\\d+)(?=\\s|$)"
    );
    private static final Pattern DECIMAL_OPTION = Pattern.compile(
            "(?i)(?:^|\\s)(?:--?)?%s(?:\\s+|=)(-?\\d+(?:\\.\\d+)?)(?=\\s|$)"
    );
    private static final Pattern STRING_OPTION = Pattern.compile(
            "(?i)(?:^|\\s)(?:--?)?%s(?:\\s+|=)([^\\s]+)"
    );

    private final JavaPlugin plugin;
    private final Consumer<String> debug;
    private final Map<String, Method> placeholderMethods = new ConcurrentHashMap<>();

    ActionExecutor(JavaPlugin plugin, Consumer<String> debug) {
        this.plugin = plugin;
        this.debug = debug;
    }

    void execute(List<String> actions, Context context) {
        long chainOffset = 0L;
        for (String rawAction : actions) {
            if (rawAction == null || rawAction.isBlank()) {
                continue;
            }
            ParsedAction action = ParsedAction.parse(replacePlaceholders(rawAction.trim(), context));
            for (int execution = 0; execution < action.executions(); execution++) {
                long delay = chainOffset + action.delay() + (long) execution * action.executionDelay();
                schedule(delay, () -> executeNow(action, context));
            }
            if (!action.doNotWait()) {
                chainOffset += action.delay()
                        + (long) Math.max(0, action.executions() - 1) * action.executionDelay()
                        + action.pause();
            }
        }
    }

    void execute(String action, Context context) {
        execute(List.of(action), context);
    }

    private void executeNow(ParsedAction parsed, Context context) {
        String action = parsed.command();
        String lower = action.toLowerCase(Locale.ROOT);
        Location location = actionLocation(parsed, context);
        try {
            if (lower.startsWith("execute -c ")) {
                dispatchConsole(action.substring("execute -c ".length()));
            } else if (lower.startsWith("execute -p ")) {
                context.player().performCommand(stripSlash(action.substring("execute -p ".length())));
            } else if (lower.startsWith("group ")) {
                executeGroup(context.player(), action);
            } else if (lower.startsWith("message ")) {
                executeMessage(context.player(), action);
            } else if (lower.startsWith("title ")) {
                executeTitle(context.player(), action);
            } else if (lower.startsWith("sound --play ")) {
                playSound(location, action.substring("sound --play ".length()));
            } else if (lower.startsWith("particle --play ")) {
                playParticle(location, action.substring("particle --play ".length()));
            } else if (lower.startsWith("thunderbolt")) {
                executeThunderbolt(location, action);
            } else if (lower.startsWith("firework")) {
                executeFirework(location, action);
            } else if (lower.startsWith("test-group ")) {
                executeGroup(context.player(), "group " + action.substring("test-group ".length()));
            } else if (action.startsWith("/")) {
                dispatchConsole(action);
            } else {
                debug.accept("Unknown action: " + action);
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to execute action '" + action + "': " + exception.getMessage());
        }
    }

    private void executeMessage(Player player, String action) {
        boolean broadcast = containsFlag(action, "broadcast");
        boolean skipUser = containsFlag(action, "skip-user");
        String text = color(textAfterFlag(action, "-t"));
        if (!broadcast) {
            player.sendMessage(text);
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!skipUser || !online.getUniqueId().equals(player.getUniqueId())) {
                online.sendMessage(text);
            }
        }
        Bukkit.getConsoleSender().sendMessage(text);
    }

    private void executeTitle(Player player, String action) {
        boolean broadcast = containsFlag(action, "broadcast");
        boolean skipUser = containsFlag(action, "skip-user");
        String title = color(textBetweenFlags(action, "-f", "-s"));
        String subtitle = color(textAfterFlag(action, "-s"));
        int fadeIn = optionInt(action, "fade-in", 10);
        int stay = optionInt(action, "stay", 70);
        int fadeOut = optionInt(action, "fade-out", 20);
        if (!broadcast) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!skipUser || !online.getUniqueId().equals(player.getUniqueId())) {
                online.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    private void executeGroup(Player player, String action) {
        String lower = action.toLowerCase(Locale.ROOT);
        String marker = lower.contains("--safe-set") ? "--safe-set" : lower.contains("--set") ? "--set" : "";
        if (marker.isBlank()) {
            debug.accept("Group action is missing --safe-set or --set: " + action);
            return;
        }
        int markerIndex = lower.indexOf(marker);
        String group = action.substring(markerIndex + marker.length()).trim().split("\\s+")[0];
        if (group.isBlank()) {
            return;
        }
        String template = plugin.getConfig().getString("lp_group_set", "lp user %user% group set %group%");
        dispatchConsole(template
                .replace("%user%", player.getName())
                .replace("%username%", player.getName())
                .replace("%group%", group));
    }

    private void playSound(Location location, String arguments) {
        String soundName = arguments.trim().split("\\s+")[0];
        float volume = (float) optionDouble(arguments, "volume", 1.0D);
        float pitch = (float) optionDouble(arguments, "pitch", 1.0D);
        String keyValue = soundName.toLowerCase(Locale.ROOT).replace('_', '.');
        NamespacedKey key = NamespacedKey.fromString(keyValue.contains(":") ? keyValue : "minecraft:" + keyValue);
        Sound sound = key == null ? null : Registry.SOUNDS.get(key);
        if (sound == null) {
            debug.accept("Unknown sound: " + soundName);
            return;
        }
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    private void playParticle(Location location, String arguments) {
        String particleName = arguments.trim().split("\\s+")[0].replace("#", "").toUpperCase(Locale.ROOT);
        particleName = switch (particleName) {
            case "EXPLOSION_LARGE", "EXPLOSION_HUGE" -> "EXPLOSION";
            case "EXPLOSION_NORMAL" -> "POOF";
            case "REDSTONE" -> "DUST";
            default -> particleName;
        };
        int count = optionInt(arguments, "count", 24);
        double spread = optionDouble(arguments, "spread", 0.35D);
        Particle particle = Particle.valueOf(particleName);
        if (particle == Particle.DUST) {
            location.getWorld().spawnParticle(
                    particle, location, count, spread, spread, spread, 0.01D,
                    new Particle.DustOptions(Color.RED, 1.2F)
            );
        } else {
            location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, 0.01D);
        }
    }

    private void executeThunderbolt(Location base, String action) {
        double scatter = optionDouble(action, "scatter", 0.0D);
        Location location = base.clone().add(
                randomOffset(scatter),
                0.0D,
                randomOffset(scatter)
        );
        if (containsFlag(action, "effect")) {
            location.getWorld().strikeLightningEffect(location);
        } else {
            location.getWorld().strikeLightning(location);
        }
    }

    private void executeFirework(Location location, String action) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect.Type type;
        try {
            type = FireworkEffect.Type.valueOf(optionString(action, "shape", "BALL").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            type = FireworkEffect.Type.BALL;
        }
        FireworkEffect.Builder effect = FireworkEffect.builder()
                .with(type)
                .flicker(containsFlag(action, "flicker"))
                .trail(containsFlag(action, "trail"));
        List<Color> colors = parseColors(optionString(action, "colors", "RED"));
        effect.withColor(colors);
        meta.addEffect(effect.build());
        meta.setPower(Math.max(0, Math.min(4, optionInt(action, "power", 1))));
        firework.setFireworkMeta(meta);
    }

    private List<Color> parseColors(String value) {
        List<Color> colors = new ArrayList<>();
        for (String raw : value.split(",")) {
            colors.add(switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "AQUA" -> Color.AQUA;
                case "BLACK" -> Color.BLACK;
                case "BLUE" -> Color.BLUE;
                case "FUCHSIA", "MAGENTA" -> Color.FUCHSIA;
                case "GRAY", "GREY" -> Color.GRAY;
                case "GREEN" -> Color.GREEN;
                case "LIME" -> Color.LIME;
                case "MAROON" -> Color.MAROON;
                case "NAVY" -> Color.NAVY;
                case "OLIVE" -> Color.OLIVE;
                case "ORANGE" -> Color.ORANGE;
                case "PURPLE" -> Color.PURPLE;
                case "SILVER" -> Color.SILVER;
                case "TEAL" -> Color.TEAL;
                case "WHITE" -> Color.WHITE;
                case "YELLOW" -> Color.YELLOW;
                default -> Color.RED;
            });
        }
        return colors;
    }

    private Location actionLocation(ParsedAction action, Context context) {
        Location base = action.executeFromMiddle() && context.middle() != null
                ? context.middle().clone()
                : context.source().clone();
        return base.add(action.offset());
    }

    private String replacePlaceholders(String input, Context context) {
        CaseAward award = context.award();
        String output = input
                .replace("%username%", context.player().getName())
                .replace("%user%", context.player().getName())
                .replace("%player%", context.player().getName())
                .replace("%uuid%", context.player().getUniqueId().toString())
                .replace("%box%", context.box().getId())
                .replace("%box_name%", context.box().getName())
                .replace("%award%", award == null ? "" : award.getId())
                .replace("%award_name%", award == null ? "" : award.getHologramText())
                .replace("%chance%", award == null ? "0" : String.valueOf(award.getChance()));
        for (Map.Entry<String, Object> entry : context.parameters().entrySet()) {
            String value = String.valueOf(entry.getValue());
            output = output
                    .replace("%" + entry.getKey() + "%", value)
                    .replace("%point_" + entry.getKey() + "%", value);
        }
        return applyPlaceholderApi(context.player(), output);
    }

    private String applyPlaceholderApi(Player player, String text) {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            return text;
        }
        try {
            Method method = placeholderMethods.computeIfAbsent("setPlaceholders", ignored -> {
                try {
                    return Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                            .getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                } catch (ReflectiveOperationException exception) {
                    return null;
                }
            });
            return method == null ? text : (String) method.invoke(null, player, text);
        } catch (ReflectiveOperationException exception) {
            debug.accept("PlaceholderAPI hook failed: " + exception.getMessage());
            return text;
        }
    }

    private void dispatchConsole(String command) {
        String stripped = stripSlash(command.trim());
        if (!stripped.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripped);
        }
    }

    private void schedule(long delay, Runnable task) {
        if (delay <= 0L && Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(0L, delay));
        }
    }

    private static boolean containsFlag(String value, String flag) {
        return Pattern.compile("(?i)(?:^|\\s)--?" + Pattern.quote(flag) + "(?=\\s|$)")
                .matcher(value)
                .find();
    }

    private static int optionInt(String value, String option, int fallback) {
        Matcher matcher = Pattern.compile(INTEGER_OPTION.pattern().formatted(Pattern.quote(option))).matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private static double optionDouble(String value, String option, double fallback) {
        Matcher matcher = Pattern.compile(DECIMAL_OPTION.pattern().formatted(Pattern.quote(option))).matcher(value);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private static String optionString(String value, String option, String fallback) {
        Matcher matcher = Pattern.compile(STRING_OPTION.pattern().formatted(Pattern.quote(option))).matcher(value);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static String textAfterFlag(String text, String flag) {
        int index = text.toLowerCase(Locale.ROOT).indexOf(flag.toLowerCase(Locale.ROOT));
        return index < 0 ? "" : text.substring(index + flag.length()).trim();
    }

    private static String textBetweenFlags(String text, String startFlag, String endFlag) {
        int start = text.toLowerCase(Locale.ROOT).indexOf(startFlag.toLowerCase(Locale.ROOT));
        if (start < 0) {
            return "";
        }
        start += startFlag.length();
        int end = text.toLowerCase(Locale.ROOT).indexOf(endFlag.toLowerCase(Locale.ROOT), start);
        return end < 0 ? text.substring(start).trim() : text.substring(start, end).trim();
    }

    private static String stripSlash(String command) {
        String stripped = command;
        while (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        return stripped;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private static double randomOffset(double radius) {
        return radius <= 0.0D ? 0.0D : (Math.random() * 2.0D - 1.0D) * radius;
    }

    record Context(
            Player player,
            CaseBox box,
            CaseAward award,
            Location source,
            Location middle,
            Map<String, Object> parameters
    ) {
        Context(Player player, CaseBox box, CaseAward award, Location source, Location middle) {
            this(player, box, award, source, middle, Map.of());
        }

        Context {
            if (source == null) {
                source = player.getLocation();
            }
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        }
    }

    record ParsedAction(
            String command,
            int executions,
            int executionDelay,
            int delay,
            int pause,
            Vector offset,
            boolean executeFromMiddle,
            boolean doNotWait
    ) {
        static ParsedAction parse(String action) {
            String offsetValue = optionString(action, "offset", "0,0,0");
            String[] coordinates = offsetValue.split(",");
            Vector offset = new Vector();
            if (coordinates.length == 3) {
                try {
                    offset.setX(Double.parseDouble(coordinates[0]));
                    offset.setY(Double.parseDouble(coordinates[1]));
                    offset.setZ(Double.parseDouble(coordinates[2]));
                } catch (NumberFormatException ignored) {
                    offset.zero();
                }
            }
            return new ParsedAction(
                    action,
                    Math.max(1, optionInt(action, "executions", 1)),
                    Math.max(0, optionInt(action, "exec-delay", 0)),
                    Math.max(0, optionInt(action, "delay", 0)),
                    Math.max(0, optionInt(action, "pause", 0)),
                    offset,
                    containsFlag(action, "exec-middle"),
                    containsFlag(action, "dnw")
            );
        }
    }
}
