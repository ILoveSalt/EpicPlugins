package hgds.epicgrief.bosses;

import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.TreeMap;

public record BossDefinition(
        String id,
        String name,
        EntityType type,
        LocationDefinition location,
        double maxHealth,
        PushOptions push,
        DamageOptions damage,
        List<String> spawnCommands,
        List<String> rewardCommands,
        List<String> timeoutCommands,
        int lifeTimeSeconds,
        SpawnMobOptions spawnMob,
        List<ItemReward> itemRewards,
        TreeMap<Integer, StageOptions> stages,
        List<AbilityDefinition> abilities
) {

    public StageMatch stageFor(double healthPercent) {
        for (Integer threshold : stages.keySet()) {
            if (healthPercent <= threshold) {
                return new StageMatch(threshold, stages.get(threshold));
            }
        }

        return null;
    }

    public record StageMatch(int threshold, StageOptions options) {
    }
}
