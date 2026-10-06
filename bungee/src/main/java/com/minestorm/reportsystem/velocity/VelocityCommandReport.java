package com.minestorm.reportsystem.velocity;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.minestorm.reportsystem.common.model.ReportCategory;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;

public final class VelocityCommandReport implements SimpleCommand {
    private final VelocityMain plugin;
    private final DatabaseManager db;
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacyAmpersand();

    public VelocityCommandReport(VelocityMain plugin, DatabaseManager db) {
        this.plugin = plugin; this.db = db;
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        if (!(src instanceof Player)) { src.sendMessage(Component.text("Players only.")); return; }
        Player p = (Player) src;
        String[] args = inv.arguments();
        if (args.length != 1) { p.sendMessage(L.deserialize("&cUsage: /report <player>")); return; }
        Optional<Player> target = plugin.server().getPlayer(args[0]);
        if (!target.isPresent()) { p.sendMessage(L.deserialize("&cThat player is not online.")); return; }
        if (target.get().getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(L.deserialize("&cYou cannot report yourself.")); return;
        }
        db.insertReport(p.getUniqueId(), p.getUsername(),
                        target.get().getUniqueId(), target.get().getUsername(),
                        ReportCategory.CHAT_ABUSE, System.currentTimeMillis(), true)
          .whenComplete((id, err) -> {
              if (err != null) p.sendMessage(L.deserialize("&cReport failed: " + err.getMessage()));
              else if (id == -1) p.sendMessage(L.deserialize("&cYou already have an open report against that player."));
              else p.sendMessage(L.deserialize("&aReport submitted against " + target.get().getUsername() + " (#" + id + ")"));
          });
    }
}
