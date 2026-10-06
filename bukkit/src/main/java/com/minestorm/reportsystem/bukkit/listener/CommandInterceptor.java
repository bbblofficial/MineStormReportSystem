package com.minestorm.reportsystem.bukkit.listener;

import com.minestorm.reportsystem.bukkit.BukkitMain;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * اینترسپتور /report و /my-task.
 *
 * روی LOWEST اجرا می‌شود تا قبل از MyCommand، Essentials، CustomCommands
 * و هر پلاگین دیگری دستور را کنسل کند.
 */
public final class CommandInterceptor implements Listener {

    private final BukkitMain plugin;

    private static final Set<String> REPORT_LABELS = new HashSet<>(Arrays.asList(
            "report", "reportplayer", "report-player",
            "report_player", "reports", "reportuser"
    ));

    private static final Set<String> TASK_LABELS = new HashSet<>(Arrays.asList(
            "my-task", "mytask", "my_task", "mytasks",
            "reports-admin", "reportsadmin", "adminreport"
    ));

    public CommandInterceptor(BukkitMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.isEmpty() || raw.charAt(0) != '/') return;

        String body = raw.substring(1).trim();
        if (body.isEmpty()) return;

        int space = body.indexOf(' ');
        String label = (space < 0) ? body : body.substring(0, space);
        String args = (space < 0) ? "" : body.substring(space + 1).trim();

        // حذف namespace (مثل /essentials:report)
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        label = label.toLowerCase(Locale.ROOT);

        boolean isReport = REPORT_LABELS.contains(label);
        boolean isTask = TASK_LABELS.contains(label);

        if (!isReport && !isTask) return;

        // کنسل کامل — هیچ پلاگین دیگری اجرا نمی‌شود
        event.setCancelled(true);

        Player p = event.getPlayer();

        if (isReport) {
            if (args.isEmpty()) {
                p.sendMessage(color("&8[&cReports&8] &7Usage: &f/report <player>"));
            } else {
                p.sendMessage(color("&8[&cReports&8] &7MineStorm Report System is active. "
                        + "Use &f/report " + args + " &7to report."));
            }
        } else {
            p.sendMessage(color("&8[&cReports&8] &7Use &f/my-task &7to view reports."));
        }

        plugin.getLogger().info("[MSRS] Intercepted /" + label + " from " + p.getName());
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
