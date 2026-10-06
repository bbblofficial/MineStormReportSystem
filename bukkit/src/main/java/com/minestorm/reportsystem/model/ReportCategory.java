package com.minestorm.reportsystem.model;

import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

public enum ReportCategory {
    CHAT_ABUSE(Material.BOOK, "Chat Abuse / Spamming", "Toxic or offensive messages,", "advertising or flooding chat."),
    CHEATING(Material.DIAMOND_SWORD, "Cheating / Hacking", "Using hacked clients or mods", "that give unfair advantage."),
    BAD_NAME(Material.NAME_TAG, "Bad Name", "Username offensive, hateful", "or otherwise inappropriate."),
    GUILD_NAME(Material.BANNER, "Guild Name / Tag", "Guild name/tag offensive", "or breaks the rules."),
    CROSS_TEAMING(Material.COMPASS, "Cross-Teaming", "Teaming with enemies in", "modes where not allowed."),
    BAD_SKIN(Material.LEATHER, "Bad Skin / Cape", "Skin or cape that is", "explicit or offensive."),
    STATS_BOOSTING(Material.TNT, "Stats Boosting", "Farming wins/kills/levels", "to boost stats unfairly.");

    private final Material icon;
    private final String displayName;
    private final List<String> description;

    ReportCategory(Material icon, String displayName, String... desc) {
        this.icon = icon;
        this.displayName = displayName;
        this.description = Arrays.asList(desc);
    }
    public Material getIcon() { return icon; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }

    public static ReportCategory fromName(String n) {
        if (n == null) return null;
        try { return valueOf(n.toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }
}
