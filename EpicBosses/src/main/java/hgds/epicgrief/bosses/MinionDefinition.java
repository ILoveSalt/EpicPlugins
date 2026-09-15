package hgds.epicgrief.bosses;

import org.bukkit.entity.EntityType;

import java.util.Locale;

public record MinionDefinition(EntityType type, double chance, double health) {

    public static MinionDefinition parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split(";");
        if (parts.length < 1) {
            return null;
        }

        try {
            EntityType type = EntityType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            if (!type.isAlive()) {
                return null;
            }

            double chance = parts.length >= 2 ? parseDouble(parts[1], 100.0D) : 100.0D;
            double health = parts.length >= 3 ? parseDouble(parts[2], 0.0D) : 0.0D;
            return new MinionDefinition(type, Math.max(0.0D, Math.min(100.0D, chance)), Math.max(0.0D, health));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
