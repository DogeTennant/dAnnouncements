package com.dogetennant.dannouncements.util;

import java.time.ZoneId;

public final class TimeUtil {

    private TimeUtil() {}

    /** Resolves a config timezone string ("system" or a Java ZoneId) with a safe fallback. */
    public static ZoneId resolveTimezone(String tz) {
        if (tz == null || tz.equalsIgnoreCase("system")) {
            ZoneId id = ZoneId.systemDefault();
            LogUtil.info("Timezone: system -> " + id.getId());
            return id;
        }
        try {
            ZoneId id = ZoneId.of(tz);
            LogUtil.info("Timezone: " + id.getId());
            return id;
        } catch (Exception e) {
            LogUtil.warn("Invalid timezone '" + tz + "', falling back to system default.");
            return ZoneId.systemDefault();
        }
    }

    /** Formats a duration in seconds as a human-readable string, e.g. "2d 1h 23m 45s". */
    public static String format(long seconds) {
        if (seconds <= 0) return "0s";

        long days    = seconds / 86400;
        long hours   = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.isEmpty()) sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
