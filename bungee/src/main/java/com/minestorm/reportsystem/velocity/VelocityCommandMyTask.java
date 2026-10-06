package com.minestorm.reportsystem.velocity;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityCommandMyTask implements SimpleCommand {
    private final VelocityMain plugin;
    private final DatabaseManager db;
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacyAmpersand();

    public VelocityCommandMyTask(VelocityMain plugin, DatabaseManager db) {
        this.plugin = plugin; this.db = db;
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        if (!(src instanceof Player)) { src.sendMessage(L.deserialize("&cPlayers only.")); return; }
        Player p = (Player) src;
        if (!p.hasPermission("minestorm.report.admin")) {
            p.sendMessage(L.deserialize("&cNo permission.")); return;
        }
        String[] args = inv.arguments();
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            p.sendMessage(L.deserialize("&aReloaded.")); return;
        }
        db.getReports().whenComplete((reports, err) -> {
            if (err != null) { p.sendMessage(L.deserialize("&cDB error: " + err.getMessage())); return; }
            p.sendMessage(L.deserialize("&eActive reports: &f" + reports.size()));
            reports.stream().limit(10).forEach(r ->
                p.sendMessage(L.deserialize("&8#&f" + r.id() + " &7"
                    + r.reporterName() + " &8→ &f" + r.targetName()
                    + " &7(" + r.category().getDisplayName() + ")")));
        });
    }
}
