package com.minestorm.reportsystem.util;

import com.minestorm.reportsystem.ReportSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final int MAX_TITLE_LENGTH = 32;
    private final ReportSystem plugin;

    public Messages(ReportSystem plugin) { this.plugin = plugin; }

    public String raw(String key, Object... replacements) {
        String template = plugin.getConfig().getString("messages." + key,
                "&cMissing message: " + key);
        return apply(template, replacements);
    }

    public String prefixed(String key, Object... replacements) {
        return raw("prefix") + raw(key, replacements);
    }

    public void send(CommandSender target, String key, Object... replacements) {
        target.sendMessage(prefixed(key, replacements));
    }

    public String title(String key, Object... replacements) {
        String template = plugin.getConfig().getString("gui.titles." + key, "&8" + key);
        String title = apply(template, replacements);
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
            if (title.endsWith(String.valueOf('\u00a7')))
                title = title.substring(0, title.length() - 1);
        }
        return title;
    }

    public static String apply(String template, Object... replacements) {
        String out = ChatColor.translateAlternateColorCodes('&', template);
        for (int i = 0; i + 1 < replacements.length; i += 2)
            out = out.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        return out;
    }
}
