package hgds.epicAntiRelog.combat;

import org.bukkit.boss.BossBar;

import java.util.UUID;

final class CombatTag {

    private final UUID playerId;
    private String playerName;
    private long endsAt;
    private BossBar bossBar;

    CombatTag(UUID playerId) {
        this.playerId = playerId;
    }

    UUID playerId() {
        return playerId;
    }

    long endsAt() {
        return endsAt;
    }

    void endsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    void playerName(String playerName) {
        this.playerName = playerName;
    }

    BossBar bossBar() {
        return bossBar;
    }

    void bossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }
}
