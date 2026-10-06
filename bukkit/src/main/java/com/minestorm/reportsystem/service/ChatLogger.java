package com.minestorm.reportsystem.service;

import java.util.function.BiConsumer;
import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.ChatChannel;
import com.minestorm.reportsystem.model.ChatRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public final class ChatLogger implements Listener {
    private final ReportSystem plugin;
    private final Queue<ChatRecord> queue = new ConcurrentLinkedQueue<ChatRecord>();
    private volatile boolean enabled = true;
    private volatile Set<String> priv = new HashSet<String>();
    private volatile Set<String> reply = new HashSet<String>();
    private volatile Set<String> guild = new HashSet<String>();

    public ChatLogger(ReportSystem p) { plugin = p; }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("chat-logging.enabled", true);
        priv = readSet("chat-logging.private-message-commands");
        reply = readSet("chat-logging.reply-commands");
        guild = readSet("chat-logging.guild-chat-commands");
    }

    private Set<String> readSet(String path) {
        Set<String> s = new HashSet<String>();
        for (String e : plugin.getConfig().getStringList(path)) s.add(e.toLowerCase(Locale.ROOT));
        return s;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!enabled) return;
        enqueue(e.getPlayer(), ChatChannel.PUBLIC, null, e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (!enabled) return;
        String m = e.getMessage();
        if (m.length() < 2) return;
        String body = m.substring(1).trim();
        String[] parts = body.split("\\s+", 3);
        String lbl = parts[0].toLowerCase(Locale.ROOT);
        int c = lbl.indexOf(':');
        if (c >= 0) lbl = lbl.substring(c + 1);
        Player p = e.getPlayer();
        if (priv.contains(lbl)) {
            if (parts.length >= 3) enqueue(p, ChatChannel.PRIVATE, parts[1], parts[2]);
        } else if (reply.contains(lbl)) {
            if (parts.length >= 2)
                enqueue(p, ChatChannel.PRIVATE, null, body.substring(parts[0].length()).trim());
        } else if (guild.contains(lbl) && parts.length >= 2) {
            enqueue(p, ChatChannel.GUILD, null, body.substring(parts[0].length()).trim());
        }
    }

    private void enqueue(Player p, ChatChannel c, String t, String m) {
        String txt = m.length() > 500 ? m.substring(0, 500) : m;
        queue.add(new ChatRecord(p.getUniqueId(), p.getName(), c, t, txt, System.currentTimeMillis()));
    }

    public void flush() {
        final List<ChatRecord> batch = drain();
        if (batch.isEmpty()) return;
        plugin.database().insertChatBatch(batch).whenComplete(
            new java.util.function.BiConsumer<Void, Throwable>() {
                public void accept(Void v, Throwable e) {
                    if (e != null) plugin.getLogger().log(Level.SEVERE,
                        "Failed to save " + batch.size() + " chat messages", e);
                }
            });
    }

    public void flushBlocking() {
        List<ChatRecord> b = drain();
        if (b.isEmpty()) return;
        try { plugin.database().insertChatBatch(b).join(); }
        catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Chat save failed on shutdown", e);
        }
    }

    private List<ChatRecord> drain() {
        List<ChatRecord> b = new ArrayList<ChatRecord>();
        ChatRecord r;
        while ((r = queue.poll()) != null) b.add(r);
        return b;
    }
}
