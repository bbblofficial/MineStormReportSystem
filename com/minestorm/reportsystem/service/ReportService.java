package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import com.minestorm.reportsystem.model.ReportOutcome;
import com.minestorm.reportsystem.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportService {
    private final ReportSystem plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<UUID, Long>();

    public ReportService(ReportSystem plugin) { this.plugin = plugin; }

    public int getRemainingCooldown(Player player) {
        if (PermissionUtil.has(player, PermissionUtil.BYPASS)) return 0;
        Long last = cooldowns.get(player.getUniqueId());
        if (last == null) return 0;
        long cooldownMillis = plugin.getConfig().getLong("report.cooldown-seconds", 60L) * 1000L;
        long remaining = last.longValue() + cooldownMillis - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) ((remaining + 999L) / 1000L);
    }

    public void clearCooldown(UUID uuid) { cooldowns.remove(uuid); }

    public void submit(final Player reporter, final String targetName,
                       final UUID targetUuid, final ReportCategory category) {
        int remaining = getRemainingCooldown(reporter);
        if (remaining > 0) {
            plugin.messages().send(reporter, "cooldown", "{seconds}", Integer.valueOf(remaining));
            return;
        }
        boolean allowDuplicates = plugin.getConfig().getBoolean("report.allow-duplicate-pending", false);
        final UUID reporterUuid = reporter.getUniqueId();
        final String reporterName = reporter.getName();
        final long now = System.currentTimeMillis();

        plugin.callback(
            plugin.database().insertReport(reporterUuid, reporterName, targetUuid,
                    targetName, category, now, !allowDuplicates),
            new java.util.function.Consumer<Integer>() {
                public void accept(Integer id) {
                    if (id.intValue() < 0) {
                        plugin.messages().send(reporter, "already-reported");
                        return;
                    }
                    cooldowns.put(reporterUuid, Long.valueOf(System.currentTimeMillis()));
                    Report report = new Report(id.intValue(), reporterUuid, reporterName,
                            targetUuid, targetName, category, now);
                    plugin.messages().send(reporter, "report-submitted",
                            "{player}", targetName,
                            "{category}", category.getDisplayName());
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (PermissionUtil.has(online, PermissionUtil.ADMIN)) {
                            plugin.messages().send(online, "admin-alert",
                                    "{reporter}", reporterName,
                                    "{player}", targetName,
                                    "{category}", category.getDisplayName());
                        }
                    }
                    plugin.webhook().reportCreated(report);
                }
            },
            new Runnable() {
                public void run() { plugin.messages().send(reporter, "report-failed"); }
            });
    }

    public void resolve(final Player admin, final Report report, final ReportOutcome outcome,
                        final Runnable afterwards) {
        PunishmentManager.Punishment punishment = null;
        if (outcome == ReportOutcome.APPROVED) {
            punishment = plugin.punishments().get(report.category());
            if (punishment == null) {
                plugin.messages().send(admin, "no-punishment",
                        "{category}", report.category().getDisplayName());
                afterwards.run();
                return;
            }
        }
        final PunishmentManager.Punishment finalPunishment = punishment;

        plugin.callback(plugin.database().deleteReport(report.id()),
            new java.util.function.Consumer<Boolean>() {
                public void accept(Boolean deleted) {
                    if (!deleted.booleanValue()) {
                        plugin.messages().send(admin, "report-already-handled");
                        afterwards.run();
                        return;
                    }
                    String command = null;
                    switch (outcome) {
                        case APPROVED:
                            command = plugin.punishments().execute(admin, report, finalPunishment);
                            plugin.messages().send(admin, "admin-approved",
                                    "{id}", Integer.valueOf(report.id()),
                                    "{command}", command);
                            break;
                        case REJECTED:
                            plugin.messages().send(admin, "admin-rejected",
                                    "{id}", Integer.valueOf(report.id()));
                            break;
                        case INVALID_CATEGORY:
                            plugin.messages().send(admin, "admin-invalid",
                                    "{id}", Integer.valueOf(report.id()));
                            break;
                    }
                    Player reporter = Bukkit.getPlayer(report.reporterUuid());
                    if (reporter != null) {
                        String key;
                        switch (outcome) {
                            case APPROVED: key = "reporter-approved"; break;
                            case REJECTED: key = "reporter-rejected"; break;
                            default: key = "reporter-invalid";
                        }
                        plugin.messages().send(reporter, key,
                                "{player}", report.targetName());
                    }
                    plugin.webhook().reportResolved(report, admin.getName(), outcome, command);
                    afterwards.run();
                }
            },
            new Runnable() {
                public void run() {
                    plugin.messages().send(admin, "database-error");
                    afterwards.run();
                }
            });
    }
}
