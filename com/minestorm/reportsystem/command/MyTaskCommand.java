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

    public MyTaskCommand(ReportSystem plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!PermissionUtil.has(sender, PermissionUtil.ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.messages().send(sender, "reloaded");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        AdminListGui.open(plugin, (Player) sender, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> matches = new ArrayList<String>();
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())
                && PermissionUtil.has(sender, PermissionUtil.ADMIN))
            matches.add("reload");
        return matches;
    }
}
