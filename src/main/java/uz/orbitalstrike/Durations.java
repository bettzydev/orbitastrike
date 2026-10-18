package uz.orbitalstrike;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Durations {
    private static final Pattern P = Pattern.compile("^(\\d+)([smhdw])$");

    private Durations() {}

    /** -2 = kick only, -1 = permanent, otherwise milliseconds. Throws IllegalArgumentException if invalid. */
    static long parse(String s) {
        String t = s.trim().toLowerCase();
        if (t.equals("-2")) return -2;
        if (t.equals("-1")) return -1;
        Matcher m = P.matcher(t);
        if (!m.matches()) throw new IllegalArgumentException("Invalid duration: " + s);
        long n = Long.parseLong(m.group(1));
        long unit;
        switch (m.group(2).charAt(0)) {
            case 's': unit = 1000L; break;
            case 'm': unit = 60_000L; break;
            case 'h': unit = 3_600_000L; break;
            case 'd': unit = 86_400_000L; break;
            default: unit = 604_800_000L; break;
        }
        return n * unit;
    }

    static String format(long ms) {
        if (ms == -1) return "forever";
        if (ms == -2) return "a moment";
        long[] units = {604_800_000L, 86_400_000L, 3_600_000L, 60_000L, 1000L};
        String[] names = {"week", "day", "hour", "minute", "second"};
        for (int i = 0; i < units.length; i++) {
            if (ms >= units[i] && ms % units[i] == 0) {
                long n = ms / units[i];
                return n + " " + names[i] + (n == 1 ? "" : "s");
            }
        }
        long sec = Math.max(1, ms / 1000);
        return sec + " seconds";
    }
}
