package com.minestorm.reportsystem.util;

import com.minestorm.reportsystem.ReportSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messages {
    private final ReportSystem plugin;
    public Messages(ReportSystem plugin) { this.plugin = plugin; }

    public String raw(String key, Object... repl) {
        String t = plugin.getConfig().getString("messages." + key, "&cMissing: " + key);
        return apply(t, repl);
    }
    public String prefixed(String key, Object... repl) {
        return raw("prefix") + raw(key, repl);
    }
    public void send(CommandSender target, String key, Object... repl) {
        target.sendMessage(prefixed(key, repl));
    }
    public String title(String key, Object... repl) {
        String t = apply(plugin.getConfig().getString("gui.titles." + key, "&8" + key), repl);
        if (t.length() > 32) t = t.substring(0, 32);
        return t;
    }
    public static String apply(String t, Object... repl) {
        String out = ChatColor.translateAlternateColorCodes('&', t);
        for (int i = 0; i + 1 < repl.length; i += 2)
            out = out.replace(String.valueOf(repl[i]), String.valueOf(repl[i+1]));
        return out;
    }
}
