package hgds.epicgrief.api.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class StringUtil {
    private StringUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getNumberFormat(double number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    public static boolean startsWithIgnoreCase(String string, String prefix) {
        return (string != null && string.length() >= prefix.length() && string.regionMatches(true, 0, prefix, 0, prefix.length()));
    }
}
