package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TabAnimationService {
    private static final Pattern ANIMATION_PLACEHOLDER =
            Pattern.compile("%animation:([^%]+)%", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_COLOR =
            Pattern.compile("(?i)(?<![&\\w])#([0-9a-f]{6})");
    private static final int MAX_NESTING_DEPTH = 16;

    private final EpicAnimationChat plugin;
    private final LegacyComponentSerializer legacySerializer;

    private volatile Map<String, TabAnimation> animations = Map.of();
    private volatile long animationEpochMillis = System.currentTimeMillis();
    private File animationsFile;
    private long lastModified = Long.MIN_VALUE;

    public TabAnimationService(EpicAnimationChat plugin) {
        this.plugin = plugin;
        this.legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    public synchronized void reload() {
        animationsFile = resolveAnimationsFile();

        if (!animationsFile.isFile()) {
            animations = Map.of();
            lastModified = animationsFile.lastModified();
            plugin.getLogger().warning("TAB animations file was not found: " + animationsFile);
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(animationsFile);
        Map<String, TabAnimation> loaded = new LinkedHashMap<>();
        loadAnimations(yaml, loaded);

        ConfigurationSection nested = yaml.getConfigurationSection("animations");
        if (nested != null) {
            loadAnimations(nested, loaded);
        }

        animations = Collections.unmodifiableMap(loaded);
        animationEpochMillis = System.currentTimeMillis();
        lastModified = animationsFile.lastModified();
    }

    public synchronized void reloadIfChanged() {
        File configuredFile = resolveAnimationsFile();
        if (!configuredFile.equals(animationsFile)
                || configuredFile.lastModified() != lastModified) {
            reload();
            plugin.getLogger().info("Reloaded " + animations.size() + " TAB animations.");
        }
    }

    public Component replaceAnimations(Component component, Player source) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - animationEpochMillis);
        TextReplacementConfig replacement = TextReplacementConfig.builder()
                .match(ANIMATION_PLACEHOLDER)
                .replacement((match, ignored) -> frameComponent(match.group(1), source, elapsed))
                .build();
        return component.replaceText(replacement);
    }

    public Component getCurrentFrame(String name, Player source) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - animationEpochMillis);
        return frameComponent(name, source, elapsed);
    }

    public boolean contains(String name) {
        return animations.containsKey(normalize(name));
    }

    public int size() {
        return animations.size();
    }

    public List<String> names() {
        return new ArrayList<>(animations.values().stream().map(TabAnimation::name).toList());
    }

    public File getAnimationsFile() {
        return animationsFile == null ? resolveAnimationsFile() : animationsFile;
    }

    private Component frameComponent(String name, Player source, long elapsed) {
        String frame = resolveFrame(name, source, elapsed, new HashSet<>(), 0);
        if (frame == null) {
            return Component.text("%animation:" + name + "%");
        }

        String colored = HEX_COLOR.matcher(frame).replaceAll("&#$1");
        colored = colored.replace('§', '&');
        return legacySerializer.deserialize(colored);
    }

    private String resolveFrame(
            String name,
            Player source,
            long elapsed,
            Set<String> stack,
            int depth
    ) {
        String normalizedName = normalize(name);
        TabAnimation animation = animations.get(normalizedName);
        if (animation == null) {
            return null;
        }
        if (depth >= MAX_NESTING_DEPTH || !stack.add(normalizedName)) {
            return animation.currentFrame(elapsed);
        }

        String frame = replacePlayerPlaceholders(animation.currentFrame(elapsed), source);
        Matcher matcher = ANIMATION_PLACEHOLDER.matcher(frame);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String nested = resolveFrame(matcher.group(1), source, elapsed, stack, depth + 1);
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(nested == null ? matcher.group() : nested)
            );
        }
        matcher.appendTail(result);
        stack.remove(normalizedName);
        return result.toString();
    }

    private String replacePlayerPlaceholders(String input, Player source) {
        if (source == null) {
            return input;
        }

        return input
                .replace("%player%", source.getName())
                .replace("%player_name%", source.getName())
                .replace("%player_uuid%", source.getUniqueId().toString());
    }

    private void loadAnimations(ConfigurationSection root, Map<String, TabAnimation> target) {
        long defaultInterval = Math.max(
                1L,
                plugin.getConfig().getLong("tab.default-change-interval-millis", 100L)
        );

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null || !section.isList("texts")) {
                continue;
            }

            List<String> frames = section.getStringList("texts");
            if (frames.isEmpty()) {
                plugin.getLogger().warning("Ignoring TAB animation '" + key + "': texts is empty.");
                continue;
            }

            long interval = Math.max(1L, section.getLong("change-interval", defaultInterval));
            target.put(normalize(key), new TabAnimation(key, interval, frames));
        }
    }

    private File resolveAnimationsFile() {
        String configured = plugin.getConfig().getString(
                "tab.animations-file",
                "TAB/animations.yml"
        );
        File pluginsDirectory = plugin.getDataFolder().getParentFile();
        File file = new File(configured == null ? "TAB/animations.yml" : configured);
        if (!file.isAbsolute()) {
            file = new File(pluginsDirectory, file.getPath());
        }

        if (file.isFile()) {
            return file;
        }

        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory()) {
            String targetName = file.getName();
            File[] matches = parent.listFiles(candidate ->
                    candidate.isFile() && candidate.getName().equalsIgnoreCase(targetName)
            );
            if (matches != null && matches.length > 0) {
                return matches[0];
            }
        }
        return file;
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
