package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;

public final class CleanupTask implements Runnable {
    private static final long DAY = 86400000L;
    private final ReportSystem plugin;
    public CleanupTask(ReportSystem p) { plugin = p; }

    public void run() {
        long now = System.currentTimeMillis();
        long rc = cut(now, plugin.getConfig().getInt("cleanup.report-expiry-days", 7));
        long cc = cut(now, plugin.getConfig().getInt("cleanup.chat-log-retention-days", 14));
        long gc = cut(now, plugin.getConfig().getInt("cleanup.grim-alert-retention-days", 14));
        plugin.database().cleanup(rc, cc, gc).whenComplete(
            new java.util.function.BiConsumer<int[], Throwable>() {
                public void accept(int[] c, Throwable e) {
                    if (e != null) plugin.getLogger().warning("Cleanup failed: " + e.getMessage());
                    else if (c[0] + c[1] + c[2] > 0)
                        plugin.getLogger().info("Cleanup: " + c[0] + " reports, " + c[1] + " chats, " + c[2] + " grim.");
                }
            });
    }

    private long cut(long now, int days) { return days <= 0 ? 0 : now - days * DAY; }
}
