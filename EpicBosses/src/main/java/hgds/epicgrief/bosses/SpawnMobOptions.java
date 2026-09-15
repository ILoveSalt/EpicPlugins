package hgds.epicgrief.bosses;

import java.util.List;

public record SpawnMobOptions(int intervalSeconds, List<MinionDefinition> minions) {
}
