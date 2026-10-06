package com.minestorm.reportsystem.bukkit.listener;

import com.minestorm.reportsystem.bukkit.BukkitMain;
import com.minestorm.reportsystem.bukkit.messaging.BukkitMessaging;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class ChatListener implements Listener {
    private final BukkitMain plugin;

    public ChatListener(BukkitMain plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        BukkitMessaging.send(p, "CHAT_LOG",
            p.getUniqueId().toString(),
            p.getName(),
            "PUBLIC",
            "",
            e.getMessage(),
            String.valueOf(System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg.length() < 2) return;
        String[] parts = msg.substring(1).split("\\s+", 3);
        String label = parts[0].toLowerCase();
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        String channel = null;
        String target = "";
        if (label.equals("msg") || label.equals("tell") || label.equals("w") || label.equals("whisper")) {
            if (parts.length >= 3) { channel = "PRIVATE"; target = parts[1]; msg = parts[2]; }
        } else if (label.equals("r") || label.equals("reply")) {
            if (parts.length >= 2) { channel = "PRIVATE"; msg = msg.substring(parts[0].length()).trim(); }
        } else if (label.equals("g") || label.equals("gc") || label.equals("guildchat")) {
            if (parts.length >= 2) { channel = "GUILD"; msg = msg.substring(parts[0].length()).trim(); }
        }
        if (channel == null) return;

        Player p = e.getPlayer();
        BukkitMessaging.send(p, "CHAT_LOG",
            p.getUniqueId().toString(),
            p.getName(),
            channel,
            target,
            msg,
            String.valueOf(System.currentTimeMillis()));
    }
}
