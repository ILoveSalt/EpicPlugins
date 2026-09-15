package hgds.epicgrief;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class CaseBox {
    private final String id;
    private final String name;
    private final List<String> description;
    private final String itemSpec;
    private final String emptyItemSpec;
    private final boolean useKeys;
    private final int keyWithdrawalAmount;
    private final String selectionMode;
    private final AnimationSettings animationSettings;
    private final List<String> onOpenActions;
    private final List<String> onAwardActions;
    private final List<String> endActions;
    private final List<CaseAward> awards;

    private CaseBox(
            String id,
            String name,
            List<String> description,
            String itemSpec,
            String emptyItemSpec,
            boolean useKeys,
            int keyWithdrawalAmount,
            String selectionMode,
            AnimationSettings animationSettings,
            List<String> onOpenActions,
            List<String> onAwardActions,
            List<String> endActions,
            List<CaseAward> awards
    ) {
        this.id = id;
        this.name = name;
        this.description = List.copyOf(description);
        this.itemSpec = itemSpec;
        this.emptyItemSpec = emptyItemSpec;
        this.useKeys = useKeys;
        this.keyWithdrawalAmount = Math.max(1, keyWithdrawalAmount);
        this.selectionMode = selectionMode == null ? "legacy" : selectionMode.toLowerCase(Locale.ROOT);
        this.animationSettings = animationSettings;
        this.onOpenActions = List.copyOf(onOpenActions);
        this.onAwardActions = List.copyOf(onAwardActions);
        this.endActions = List.copyOf(endActions);
        this.awards = List.copyOf(awards);
    }

    static CaseBox fromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = stripExtension(file.getName()).toLowerCase(Locale.ROOT);
        String name = config.getString("options.name", id);
        List<String> description = config.getStringList("options.description");
        String itemSpec = config.getString("options.item", "CHEST");
        boolean useKeys = config.getBoolean("options.useKeys", false);

        List<CaseAward> awards = new ArrayList<>();
        ConfigurationSection awardsSection = config.getConfigurationSection("awards");
        if (awardsSection != null) {
            for (String awardId : awardsSection.getKeys(false)) {
                String path = "awards." + awardId;
                awards.add(new CaseAward(
                        awardId,
                        config.getDouble(path + ".chance", 0.0D),
                        config.getBoolean(path + ".rare", false),
                        config.getBoolean(path + ".phantom", false),
                        config.getString(path + ".skip-permission"),
                        config.getString(path + ".hologramText", awardId),
                        config.getString(path + ".hologramItem", "CHEST"),
                        config.getStringList(path + ".actions")
                ));
            }
        }

        return new CaseBox(
                id,
                name,
                description,
                itemSpec,
                config.getString("options.emptyItem", itemSpec),
                useKeys,
                config.getInt("options.withdrawalAmount", 1),
                config.getString("options.selectionMode", "legacy"),
                AnimationSettings.fromConfig(config),
                config.getStringList("onOpenActions"),
                config.getStringList("onAwardActions"),
                config.getStringList("endActions"),
                awards
        );
    }

    CaseAward roll(Random random, Player player) {
        List<CaseAward> eligible = awards.stream()
                .filter(award -> !award.isPhantom())
                .filter(award -> award.getSkipPermission() == null
                        || award.getSkipPermission().isBlank()
                        || player == null
                        || !player.hasPermission(award.getSkipPermission()))
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }
        if ("legacy".equals(selectionMode)) {
            return legacyRoll(random, eligible);
        }

        double totalChance = 0.0D;
        for (CaseAward award : eligible) {
            if (award.getChance() > 0.0D) {
                totalChance += award.getChance();
            }
        }

        if (totalChance <= 0.0D) {
            return eligible.get(random.nextInt(eligible.size()));
        }

        double selected = random.nextDouble() * totalChance;
        double cursor = 0.0D;
        for (CaseAward award : eligible) {
            if (award.getChance() <= 0.0D) {
                continue;
            }

            cursor += award.getChance();
            if (selected <= cursor) {
                return award;
            }
        }

        return eligible.get(eligible.size() - 1);
    }

    private CaseAward legacyRoll(Random random, List<CaseAward> eligible) {
        List<CaseAward> candidates = new ArrayList<>(eligible);
        int maximum = candidates.stream().mapToInt(award -> (int) award.getChance()).max().orElse(0);
        if (maximum <= 0) {
            return candidates.getFirst();
        }
        int firstThreshold = random.nextInt(maximum);
        candidates.removeIf(award -> firstThreshold > award.getChance());
        maximum = candidates.stream().mapToInt(award -> (int) award.getChance()).max().orElse(0);
        if (maximum > 1) {
            int secondThreshold = random.nextInt(Math.max(1, maximum / 2));
            candidates.removeIf(award -> secondThreshold > award.getChance());
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    CaseAward roll(Random random) {
        return roll(random, null);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    List<String> getDescription() {
        return description;
    }

    String getItemSpec() {
        return itemSpec;
    }

    String getEmptyItemSpec() {
        return emptyItemSpec;
    }

    boolean isUseKeys() {
        return useKeys;
    }

    int getKeyWithdrawalAmount() {
        return keyWithdrawalAmount;
    }

    AnimationSettings getAnimationSettings() {
        return animationSettings;
    }

    List<String> getOnOpenActions() {
        return onOpenActions;
    }

    List<String> getOnAwardActions() {
        return onAwardActions;
    }

    List<String> getEndActions() {
        return endActions;
    }

    List<CaseAward> getAwards() {
        return awards;
    }
}
