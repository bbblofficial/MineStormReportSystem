package com.minestorm.reportsystem.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeUtil {
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM HH:mm:ss");
    public static String format(long millis) { return FMT.format(new Date(millis)); }
    public static String ago(long millis) {
        long s = Math.max(0, (System.currentTimeMillis() - millis) / 1000);
        if (s < 60) return s + "s ago";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        return (s / 86400) + "d ago";
    }
}
