package hgds.epicgrief.bosses;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public record BossBarSettings(String title, BarColor color, BarStyle style, int timeToDeleteSeconds) {
}
