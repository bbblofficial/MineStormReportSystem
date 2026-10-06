package com.minestorm.reportsystem.bukkit.listener;

import com.minestorm.reportsystem.bukkit.BukkitMain;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/**
 * اینترسپتور سمت بک‌اند.
 *
 * هر پلاگین دیگری روی بک‌اند که /report داشته باشه رو کنسل می‌کنه
 * و به بازیکن پیام می‌ده که از سیستم ماین‌استورم استفاده کنه.
 *
 * چرا این کار لازمه؟ چون ممکنه روی سرور بک‌اند پلاگین Report دیگه‌ای
 * نصب باشه که با /report ما تداخل داشته باشه. با این اینترسپتور
 * مطمئن می‌شیم سیستم ما برنده می‌شه.
 *
 * نکته: اگر پروکسی خودش /report رو هندل کنه، اصلاً این رویداد
 * فایر نمی‌شه. این فقط یه fallback امنیتی هست.
 */
public final class CommandInterceptor implements Listener {

    private final BukkitMain plugin;

    public CommandInterceptor(BukkitMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg == null || msg.length() < 2) return;

        // /report یا /report ...
        String body = msg.substring(1);
        int space = body.indexOf(' ');
        String label = (space < 0 ? body : body.substring(0, space))
                .toLowerCase(Locale.ROOT);
        // حذف namespace اگر هست (مثلاً plugin:report)
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        if (!label.equals("report") && !label.equals("reportplayer")
                && !label.equals("mytask") && !label.equals("my-task")) {
            return;
        }

        // اگر اینجا رسیدیم، یعنی یه پلاگین دیگه داره این دستور رو هندل می‌کنه
        // (چون پروکسی هندل نکرده). کنسلش می‌کنیم.
        event.setCancelled(true);

        Player p = event.getPlayer();

        if (label.equals("report") || label.equals("reportplayer")) {
            String args = (space < 0) ? "" : body.substring(space + 1).trim();
            if (args.isEmpty()) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8[&cReports&8] &7Usage: &f/report <player>"));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8[&cReports&8] &7This server uses MineStorm reports. "
                                + "Please use &f/report " + args + " &7(proxy)."));
            }
        } else {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&cReports&8] &7Use &f/my-task &7on the proxy to view reports."));
        }
    }
}
