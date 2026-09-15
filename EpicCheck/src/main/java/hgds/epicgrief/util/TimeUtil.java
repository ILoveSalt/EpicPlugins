package hgds.epicgrief.util;

public final class TimeUtil {
    public static long fractionalSecondsToTicks(double seconds) {
        return Math.round(seconds * 20.0D);
    }
}
