package com.minestorm.reportsystem.service;

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
    private static final int MAX_MESSAGE_LENGTH = 500;
    private final ReportSystem plugin;
    private final Queue<ChatRecord> queue = new ConcurrentLinkedQueue<ChatRecord>();

    private volatile boolean enabled = true;
    private volatile Set<String> privateCommands = new HashSet<String>();
    private volatile Set<String> replyCommands = new HashSet<String>();
    private volatile Set<String> guildCommands = new HashSet<String>();

    public ChatLogger(ReportSystem plugin) { this.plugin = plugin; }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("chat-logging.enabled", true);
        privateCommands = readSet("chat-logging.private-message-commands");
        replyCommands = readSet("chat-logging.reply-commands");
        guildCommands = readSet("chat-logging.guild-chat-commands");
    }

    private Set<String> readSet(String path) {
        Set<String> set = new HashSet<String>();
        for (String entry : plugin.getConfig().getStringList(path))
            set.add(entry.toLowerCase(Locale.ROOT));
        return set;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        enqueue(p, ChatChannel.PUBLIC, null, event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled) return;
        String message = event.getMessage();
        if (message.length() < 2) return;
        String body = message.substring(1).trim();
        String[] parts = body.split("\\s+", 3);
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        Player player = event.getPlayer();
        if (privateCommands.contains(label)) {
            if (parts.length >= 3) enqueue(player, ChatChannel.PRIVATE, parts[1], parts[2]);
        } else if (replyCommands.contains(label)) {
            if (parts.length >= 2)
                enqueue(player, ChatChannel.PRIVATE, null, body.substring(parts[0].length()).trim());
        } else if (guildCommands.contains(label)) {
            if (parts.length >= 2)
                enqueue(player, ChatChannel.GUILD, null, body.substring(parts[0].length()).trim());
        }
    }

    private void enqueue(Player player, ChatChannel channel, String target, String message) {
        String text = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
        queue.add(new ChatRecord(player.getUniqueId(), player.getName(), channel,
                target, text, System.currentTimeMillis()));
    }

    public void flush() {
        final List<ChatRecord> batch = drain();
        if (batch.isEmpty()) return;
        plugin.database().insertChatBatch(batch).whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
            public void accept(Void v, Throwable error) {
                if (error != null)
                    plugin.getLogger().log(Level.SEVERE,
                            "Failed to save " + batch.size() + " chat messages", error);
            }
        });
    }

    public void flushBlocking() {
        List<ChatRecord> batch = drain();
        if (batch.isEmpty()) return;
        try { plugin.database().insertChatBatch(batch).join(); }
        catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save chat messages on shutdown", ex);
        }
    }

    private List<ChatRecord> drain() {
        List<ChatRecord> batch = new ArrayList<ChatRecord>();
        ChatRecord r;
        while ((r = queue.poll()) != null) batch.add(r);
        return batch;
    }
}
