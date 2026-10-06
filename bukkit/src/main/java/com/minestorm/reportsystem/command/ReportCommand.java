package com.minestorm.reportsystem.command;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.gui.ReportSelectGui;
import com.minestorm.reportsystem.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ReportCommand implements CommandExecutor, TabCompleter {
    private final ReportSystem plugin;
    public ReportCommand(ReportSystem p) { plugin = p; }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) { plugin.messages().send(s, "players-only"); return true; }
        Player p = (Player) s;
        if (!PermissionUtil.has(p, PermissionUtil.USE)) {
            plugin.messages().send(p, "no-permission"); return true;
        }
        if (a.length != 1) { plugin.messages().send(p, "usage-report"); return true; }
        int rem = plugin.reports().getRemainingCooldown(p);
        if (rem > 0) {
            plugin.messages().send(p, "cooldown", "{seconds}", Integer.valueOf(rem));
            return true;
        }
        String tName; UUID tUuid;
        Player online = Bukkit.getPlayerExact(a[0]);
        if (online != null) {
            tName = online.getName(); tUuid = online.getUniqueId();
        } else if (plugin.getConfig().getBoolean("report.allow-offline-targets", true)) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(a[0]);
            if (!off.hasPlayedBefore()) { plugin.messages().send(p, "player-not-found"); return true; }
            tName = off.getName() != null ? off.getName() : a[0];
            tUuid = off.getUniqueId();
        } else {
            plugin.messages().send(p, "player-not-found"); return true;
        }
        if (tUuid.equals(p.getUniqueId())) {
            plugin.messages().send(p, "cannot-report-self"); return true;
        }
        new ReportSelectGui(plugin, tName, tUuid).open(p);
        return true;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> m = new ArrayList<String>();
        if (a.length == 1) {
            String pre = a[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(pre)) m.add(p.getName());
        }
        return m;
    }
}
