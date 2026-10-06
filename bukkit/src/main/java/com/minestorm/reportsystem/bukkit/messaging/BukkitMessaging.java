package com.minestorm.reportsystem.bukkit.messaging;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.minestorm.reportsystem.bukkit.BukkitMain;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;

public final class BukkitMessaging implements PluginMessageListener {
    private final BukkitMain plugin;

    public BukkitMessaging(BukkitMain plugin) { this.plugin = plugin; }

    public static void send(Player player, String sub, String... payload) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(sub);
        for (String s : payload) out.writeUTF(s == null ? "" : s);
        player.sendPluginMessage(BukkitMain.get(), "minestorm:rs", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"minestorm:rs".equals(channel)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sub = in.readUTF();
            if ("OPEN_GUI".equals(sub)) {
                String type = in.readUTF();
                plugin.getLogger().info("Proxy asked to open GUI '" + type + "' for " + player.getName());
                // Real implementation would deserialize inventory data and open it.
            } else if ("EXECUTE_PUNISH".equals(sub)) {
                String cmd = in.readUTF();
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), cmd);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Bad plugin message: " + ex.getMessage());
        }
    }
}
