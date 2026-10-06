package com.minestorm.reportsystem.bungee;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.minestorm.reportsystem.common.model.ReportCategory;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public final class BungeeCommandReport extends Command {
    private final BungeeMain plugin;
    private final DatabaseManager db;

    public BungeeCommandReport(BungeeMain plugin, DatabaseManager db) {
        super("report", "minestorm.report.use", "reportplayer");
        this.plugin = plugin; this.db = db;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.config().getString("messages.prefix", "&8[&cReports&8] &7");
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(prefix + "&cPlayers only.");
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) sender;
        if (args.length != 1) {
            p.sendMessage(prefix + "&cUsage: /report <player>");
            return;
        }
        String target = args[0];
        if (target.equalsIgnoreCase(p.getName())) {
            p.sendMessage(prefix + "&cYou cannot report yourself.");
            return;
        }
        ProxiedPlayer online = plugin.getProxy().getPlayer(target);
        if (online == null) {
            p.sendMessage(prefix + "&cThat player is not online on the proxy.");
            return;
        }
        // For simplicity: pick first category, real deployment opens a GUI on backend.
        db.insertReport(p.getUniqueId(), p.getName(),
                        online.getUniqueId(), online.getName(),
                        ReportCategory.CHAT_ABUSE,
                        System.currentTimeMillis(), true)
          .whenComplete((id, err) -> {
              if (err != null) p.sendMessage(prefix + "&cReport failed: " + err.getMessage());
              else if (id == -1) p.sendMessage(prefix + "&cYou already have an open report against that player.");
              else p.sendMessage(prefix + "&aYour report against " + online.getName() + " was submitted (#" + id + ")");
          });
    }
}
