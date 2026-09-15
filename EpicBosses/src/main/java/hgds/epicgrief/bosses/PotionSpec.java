package hgds.epicgrief.bosses;

import java.util.Locale;

public record PotionSpec(String type, int durationSeconds, int amplifier) {

    public static PotionSpec parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new PotionSpec("BLINDNESS", 5, 1);
        }

        String[] parts = raw.split(";");
        String type = parts[0].trim().toUpperCase(Locale.ROOT);
        int duration = parts.length >= 2 ? parseInt(parts[1], 5) : 5;
        int amplifier = parts.length >= 3 ? parseInt(parts[2], 1) : 1;
        return new PotionSpec(type, Math.max(1, duration), Math.max(0, amplifier));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
