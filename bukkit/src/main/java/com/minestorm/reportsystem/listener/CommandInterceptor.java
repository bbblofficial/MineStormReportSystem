package com.minestorm.reportsystem.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CommandInterceptor implements Listener {
    private static final Set<String> REPORT = new HashSet<String>(Arrays.asList(
        "report", "reportplayer", "reports", "reportuser", "report-player"));
    private static final Set<String> TASK = new HashSet<String>(Arrays.asList(
        "my-task", "mytask", "my_task", "mytasks", "reportsadmin"));

    private final Plugin plugin;
    public CommandInterceptor(Plugin p) { plugin = p; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCmd(PlayerCommandPreprocessEvent e) {
        String raw = e.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') return;
        String body = raw.substring(1).trim();
        if (body.isEmpty()) return;
        int sp = body.indexOf(' ');
        String lbl = sp < 0 ? body : body.substring(0, sp);
        int col = lbl.indexOf(':');
        if (col >= 0) lbl = lbl.substring(col + 1);
        lbl = lbl.toLowerCase(Locale.ROOT);
        if (!REPORT.contains(lbl) && !TASK.contains(lbl)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (REPORT.contains(lbl)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&cReports&8] &7Use &f/report <player>"));
        } else {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&cReports&8] &7Use &f/my-task"));
        }
    }
}
