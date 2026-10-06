package com.minestorm.reportsystem.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd/MM HH:mm:ss");

    public static String format(long millis) {
        synchronized (FORMAT) { return FORMAT.format(new Date(millis)); }
    }

    public static String ago(long millis) {
        long s = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        if (s < 60L) return s + "s ago";
        if (s < 3600L) return (s / 60L) + "m ago";
        if (s < 86400L) return (s / 3600L) + "h " + ((s % 3600L) / 60L) + "m ago";
        return (s / 86400L) + "d " + ((s % 86400L) / 3600L) + "h ago";
    }
}
