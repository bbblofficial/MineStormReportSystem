package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;

public final class CleanupTask implements Runnable {
    private static final long DAY_MILLIS = 86400000L;
    private final ReportSystem plugin;

    public CleanupTask(ReportSystem plugin) { this.plugin = plugin; }

    public void run() {
        long now = System.currentTimeMillis();
        long reportCutoff = cutoff(now, plugin.getConfig().getInt("cleanup.report-expiry-days", 7));
        long chatCutoff = cutoff(now, plugin.getConfig().getInt("cleanup.chat-log-retention-days", 14));
        long grimCutoff = cutoff(now, plugin.getConfig().getInt("cleanup.grim-alert-retention-days", 14));

        plugin.database().cleanup(reportCutoff, chatCutoff, grimCutoff).whenComplete(
            new java.util.function.BiConsumer<int[], Throwable>() {
                public void accept(int[] counts, Throwable error) {
                    if (error != null) plugin.getLogger().warning("Cleanup failed: " + error.getMessage());
                    else if (counts[0] + counts[1] + counts[2] > 0)
                        plugin.getLogger().info("Cleanup removed " + counts[0] + " reports, "
                                + counts[1] + " chat logs, " + counts[2] + " Grim alerts.");
                }
            });
    }

    private long cutoff(long now, int days) {
        return days <= 0 ? 0L : now - days * DAY_MILLIS;
    }
}
