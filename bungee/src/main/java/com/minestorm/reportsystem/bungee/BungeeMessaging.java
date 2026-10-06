package com.minestorm.reportsystem.bungee;

import com.google.common.io.ByteStreams;
import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.minestorm.reportsystem.common.model.ChatRecord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Collections;
import java.util.UUID;

public final class BungeeMessaging implements Listener {
    private final BungeeMain plugin;
    private final DatabaseManager db;

    public BungeeMessaging(BungeeMain plugin, DatabaseManager db) {
        this.plugin = plugin; this.db = db;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!"minestorm:rs".equals(e.getTag())) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
        try {
            String sub = in.readUTF();
            if ("CHAT_LOG".equals(sub)) {
                UUID uuid = UUID.fromString(in.readUTF());
                String name = in.readUTF();
                String channel = in.readUTF();
                String target = in.readUTF();
                String message = in.readUTF();
                long time = in.readLong();
                db.insertChatBatch(Collections.singletonList(
                    new ChatRecord(uuid, name, channel, target, message, time)));
            } else if ("REPORT_SUBMIT".equals(sub)) {
                String reporterName = in.readUTF();
                UUID targetUuid = UUID.fromString(in.readUTF());
                String targetName = in.readUTF();
                String category = in.readUTF();
                plugin.getLogger().info("[MSRS] Report from " + reporterName
                    + " -> " + targetName + " (" + category + ")");
                // Full validation/persist path is handled by BungeeCommandReport.
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[MSRS] Bad plugin message: " + ex.getMessage());
        }
    }
}
