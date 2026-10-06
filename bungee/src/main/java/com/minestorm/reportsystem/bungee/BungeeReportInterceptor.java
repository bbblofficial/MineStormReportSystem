package com.minestorm.reportsystem.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Locale;

/**
 * اینترسپتور سمت پروکسی.
 *
 * اگر پلاگین دیگه‌ای روی پروکسی هم /report داشته باشه، این کلاس
 * تلاش می‌کنه با استفاده از رویدادهای BungeeCord دستور رو بگیره.
 *
 * نکته: BungeeCord اجازه override مستقیم روی Command API رو نمی‌ده،
 * اما می‌شه از TabCompleteEvent و پیام‌های incoming استفاده کرد.
 */
public final class BungeeReportInterceptor implements Listener {

    private final BungeeMain plugin;

    public BungeeReportInterceptor(BungeeMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTabComplete(TabCompleteEvent event) {
        // فقط برای اینکه مطمئن شویم لیست تب‌کامپلیت پروکسی ماست
        if (event.getCursor() == null) return;
        String cursor = event.getCursor().toLowerCase(Locale.ROOT);
        if (cursor.startsWith("/report") || cursor.startsWith("/mytask")) {
            event.getSuggestions().clear();
        }
    }
}
