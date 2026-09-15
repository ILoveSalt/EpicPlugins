package hgds.epicgrief.api.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {
    private TimeUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);

    public static String getDateTime() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date());
        }
    }

    public static String getTimeLeft(long seconds, TimeLeftFormat timeLeftFormat) {
        switch (timeLeftFormat) {
            case MINUTES:
                return String.format(Locale.US, "%02dm %02ds",
                        seconds / 60L, seconds % 60L);
            case HOURS:
                return String.format(Locale.US, "%02dh %02dm %02ds",
                        seconds / 3600L, seconds % 3600L / 60L, seconds % 60L);
            case DAYS:
                return String.format(Locale.US, "%dd %02dh %02dm %02ds",
                        seconds / 86400L, seconds % 86400L / 3600L, seconds % 3600L / 60L, seconds % 60L);
            case WEEKS:
            default:
                return String.format(Locale.US, "%dw %dd %02dh %02dm %02ds",
                        seconds / 604800L, seconds % 604800L / 86400L, seconds % 86400L / 3600L,
                        seconds % 3600L / 60L, seconds % 60L);
        }
    }
}
