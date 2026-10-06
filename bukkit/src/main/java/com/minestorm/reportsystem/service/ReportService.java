package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import com.minestorm.reportsystem.model.ReportOutcome;
import com.minestorm.reportsystem.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ReportService {
    private final ReportSystem plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<UUID, Long>();

    public ReportService(ReportSystem p) { plugin = p; }

    public int getRemainingCooldown(Player p) {
        if (PermissionUtil.has(p, PermissionUtil.BYPASS)) return 0;
        Long last = cooldowns.get(p.getUniqueId());
        if (last == null) return 0;
        long cd = plugin.getConfig().getLong("report.cooldown-seconds", 60L) * 1000L;
        long r = last.longValue() + cd - System.currentTimeMillis();
        return r <= 0 ? 0 : (int) ((r + 999) / 1000);
    }

    public void clearCooldown(UUID u) { cooldowns.remove(u); }

    public void submit(final Player reporter, final String tName,
                       final UUID tUuid, final ReportCategory cat) {
        int r = getRemainingCooldown(reporter);
        if (r > 0) {
            plugin.messages().send(reporter, "cooldown", "{seconds}", Integer.valueOf(r));
            return;
        }
        boolean allowDup = plugin.getConfig().getBoolean("report.allow-duplicate-pending", false);
        final UUID rUuid = reporter.getUniqueId();
        final String rName = reporter.getName();
        final long now = System.currentTimeMillis();

        plugin.callback(
            plugin.database().insertReport(rUuid, rName, tUuid, tName, cat, now, !allowDup),
            new Consumer<Integer>() {
                public void accept(Integer id) {
                    if (id.intValue() < 0) {
                        plugin.messages().send(reporter, "already-reported");
                        return;
                    }
                    cooldowns.put(rUuid, Long.valueOf(System.currentTimeMillis()));
                    Report rep = new Report(id.intValue(), rUuid, rName, tUuid, tName, cat, now);
                    plugin.messages().send(reporter, "report-submitted",
                        "{player}", tName, "{category}", cat.getDisplayName());
                    for (Player o : Bukkit.getOnlinePlayers()) {
                        if (PermissionUtil.has(o, PermissionUtil.ADMIN))
                            plugin.messages().send(o, "admin-alert",
                                "{reporter}", rName,
                                "{player}", tName,
                                "{category}", cat.getDisplayName());
                    }
                    plugin.webhook().reportCreated(rep);
                }
            },
            new Runnable() { public void run() { plugin.messages().send(reporter, "report-failed"); } });
    }

    public void resolve(final Player admin, final Report report, final ReportOutcome outcome,
                        final Runnable after) {
        PunishmentManager.Punishment pun = null;
        if (outcome == ReportOutcome.APPROVED) {
            pun = plugin.punishments().get(report.category());
            if (pun == null) {
                plugin.messages().send(admin, "no-punishment",
                    "{category}", report.category().getDisplayName());
                after.run();
                return;
            }
        }
        final PunishmentManager.Punishment fp = pun;
        plugin.callback(plugin.database().deleteReport(report.id()),
            new Consumer<Boolean>() {
                public void accept(Boolean del) {
                    if (!del.booleanValue()) {
                        plugin.messages().send(admin, "report-already-handled");
                        after.run();
                        return;
                    }
                    String cmd = null;
                    switch (outcome) {
                        case APPROVED:
                            cmd = plugin.punishments().execute(admin, report, fp);
                            plugin.messages().send(admin, "admin-approved",
                                "{id}", Integer.valueOf(report.id()), "{command}", cmd);
                            break;
                        case REJECTED:
                            plugin.messages().send(admin, "admin-rejected",
                                "{id}", Integer.valueOf(report.id()));
                            break;
                        default:
                            plugin.messages().send(admin, "admin-invalid",
                                "{id}", Integer.valueOf(report.id()));
                    }
                    Player r = Bukkit.getPlayer(report.reporterUuid());
                    if (r != null) {
                        String k = outcome == ReportOutcome.APPROVED ? "reporter-approved"
                                 : outcome == ReportOutcome.REJECTED ? "reporter-rejected"
                                 : "reporter-invalid";
                        plugin.messages().send(r, k, "{player}", report.targetName());
                    }
                    plugin.webhook().reportResolved(report, admin.getName(), outcome, cmd);
                    after.run();
                }
            },
            new Runnable() {
                public void run() {
                    plugin.messages().send(admin, "database-error");
                    after.run();
                }
            });
    }
}
