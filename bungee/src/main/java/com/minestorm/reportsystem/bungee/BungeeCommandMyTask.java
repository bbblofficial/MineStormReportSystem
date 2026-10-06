package com.minestorm.reportsystem.bungee;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public final class BungeeCommandMyTask extends Command {
    private final BungeeMain plugin;
    private final DatabaseManager db;

    public BungeeCommandMyTask(BungeeMain plugin, DatabaseManager db) {
        super("my-task", "minestorm.report.admin", "mytask");
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

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            p.sendMessage(prefix + "&aReloaded.");
            return;
        }

        db.getReports().whenComplete((reports, err) -> {
            if (err != null) {
                p.sendMessage(prefix + "&cDatabase error: " + err.getMessage());
                return;
            }
            p.sendMessage(prefix + "&eActive reports: &f" + reports.size());
            reports.stream().limit(10).forEach(r ->
                p.sendMessage("&8#&f" + r.id() + " &7"
                    + r.reporterName() + " &8→ &f" + r.targetName()
                    + " &7(" + r.category().getDisplayName() + ")"));
        });
    }
}
