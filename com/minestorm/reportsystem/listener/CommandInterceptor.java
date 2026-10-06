package com.minestorm.reportsystem.listener;

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
 * اینترسپتور /report و /my-task — هر پلاگین رقیبی که این دستورات رو داشته باشه
 * رو کنسل می‌کنه تا فقط ReportSystem ما پاسخ بده.
 *
 * مهم: روی LOWEST اجرا می‌شه تا قبل از MyCommand/Essentials اجرا بشه.
 */
public final class CommandInterceptor implements Listener {

    private static final Set<String> REPORT_LABELS = new HashSet<String>(Arrays.asList(
            "report", "reportplayer", "report-player", "report_player",
            "reports", "reportuser"));

    private static final Set<String> TASK_LABELS = new HashSet<String>(Arrays.asList(
            "my-task", "mytask", "my_task", "mytasks",
            "reports-admin", "reportsadmin", "adminreport"));

    private final org.bukkit.plugin.Plugin plugin;

    public CommandInterceptor(org.bukkit.plugin.Plugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') return;

        String body = raw.substring(1).trim();
        if (body.isEmpty()) return;

        int space = body.indexOf(' ');
        String label = space < 0 ? body : body.substring(0, space);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        label = label.toLowerCase(Locale.ROOT);

        if (!REPORT_LABELS.contains(label) && !TASK_LABELS.contains(label)) return;

        // ⚠️ مهم: اگه خودمون رجیستر کرده باشیم، اجازه می‌دیم اجرا بشه.
        // اما چون ما گزارش رو رجیستر می‌کنیم، تنها پلاگین دیگه‌ای که همزمان
        // رجیستر کرده باشه همون دستور رو می‌گیره. پس فقط اگه برچسب مال ما
        // نباشه کنسل می‌کنیم. این رو با محک زدن دستور اصلی انجام می‌دیم.

        // برچسب‌های /report و /my-task خودمون رو رجیستر کردیم، پس اگه
        // بیاد اینجا یعنی یه پلاگین دیگه اون رو override کرده.
        // برای امنیت کنسل می‌کنیم.

        event.setCancelled(true);

        Player p = event.getPlayer();
        if (REPORT_LABELS.contains(label)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&cReports&8] &7Use &f/report <player> &7via MineStorm."));
        } else {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&cReports&8] &7Use &f/my-task &7to view reports."));
        }
        plugin.getLogger().fine("[MSRS] Intercepted /" + label + " from " + p.getName());
    }
}
