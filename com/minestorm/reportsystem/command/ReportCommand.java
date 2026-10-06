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

    public ReportCommand(ReportSystem plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;
        if (!PermissionUtil.has(player, PermissionUtil.USE)) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(player, "usage-report");
            return true;
        }
        int remaining = plugin.reports().getRemainingCooldown(player);
        if (remaining > 0) {
            plugin.messages().send(player, "cooldown", "{seconds}", Integer.valueOf(remaining));
            return true;
        }

        String targetName;
        UUID targetUuid;
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            targetName = online.getName();
            targetUuid = online.getUniqueId();
        } else if (plugin.getConfig().getBoolean("report.allow-offline-targets", true)) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (!offline.hasPlayedBefore()) {
                plugin.messages().send(player, "player-not-found");
                return true;
            }
            targetName = offline.getName() != null ? offline.getName() : args[0];
            targetUuid = offline.getUniqueId();
        } else {
            plugin.messages().send(player, "player-not-found");
            return true;
        }
        if (targetUuid.equals(player.getUniqueId())) {
            plugin.messages().send(player, "cannot-report-self");
            return true;
        }
        new ReportSelectGui(plugin, targetName, targetUuid).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> matches = new ArrayList<String>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix))
                    matches.add(online.getName());
            }
        }
        return matches;
    }
}
