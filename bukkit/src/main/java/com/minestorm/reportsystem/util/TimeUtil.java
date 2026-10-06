package com.minestorm.reportsystem.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeUtil {
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM HH:mm:ss");
    public static String format(long m) { synchronized (FMT) { return FMT.format(new Date(m)); } }
    public static String ago(long m) {
        long s = Math.max(0, (System.currentTimeMillis() - m) / 1000);
        if (s < 60) return s + "s ago";
        if (s < 3600) return (s/60) + "m ago";
        if (s < 86400) return (s/3600) + "h ago";
        return (s/86400) + "d ago";
    }
}
