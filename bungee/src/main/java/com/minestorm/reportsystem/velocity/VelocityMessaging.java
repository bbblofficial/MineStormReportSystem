package com.minestorm.reportsystem.velocity;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.minestorm.reportsystem.common.model.ChatRecord;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Collections;
import java.util.UUID;

public final class VelocityMessaging {
    private static final MinecraftChannelIdentifier CH =
        MinecraftChannelIdentifier.create("minestorm", "rs");

    private final VelocityMain plugin;
    private final DatabaseManager db;

    public VelocityMessaging(VelocityMain plugin, DatabaseManager db) {
        this.plugin = plugin; this.db = db;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!CH.equals(e.getIdentifier())) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()))) {
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
            }
        } catch (Exception ex) {
            plugin.logger().warn("Bad plugin message: {}", ex.getMessage());
        }
    }
}
