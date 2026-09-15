package hgds.epicgrief.arrows;

import java.util.Locale;

public enum ArrowType {
    AIM,
    EXPLODE,
    TELEPORT,
    CLEAR,
    MIX_ITEMS,
    CHANGE,
    ICE;

    public static ArrowType from(String value) {
        if (value == null || value.isBlank()) {
            return CLEAR;
        }

        try {
            return ArrowType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CLEAR;
        }
    }
}
