package com.minestorm.reportsystem.command;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.gui.AdminListGui;
import com.minestorm.reportsystem.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MyTaskCommand implements CommandExecutor, TabCompleter {
    private final ReportSystem plugin;
    public MyTaskCommand(ReportSystem p) { plugin = p; }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!PermissionUtil.has(s, PermissionUtil.ADMIN)) {
            plugin.messages().send(s, "no-permission"); return true;
        }
        if (a.length == 1 && a[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.messages().send(s, "reloaded"); return true;
        }
        if (!(s instanceof Player)) { plugin.messages().send(s, "players-only"); return true; }
        AdminListGui.open(plugin, (Player) s, 0);
        return true;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> m = new ArrayList<String>();
        if (a.length == 1 && "reload".startsWith(a[0].toLowerCase())
            && PermissionUtil.has(s, PermissionUtil.ADMIN)) m.add("reload");
        return m;
    }
}
