package com.minestorm.reportsystem.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ReportCategory {
    CHAT_ABUSE(Material.BOOK, "Chat Abuse / Spamming",
        "Toxic or offensive messages,", "advertising, or flooding the chat."),
    CHEATING(Material.DIAMOND_SWORD, "Cheating / Hacking",
        "Using hacked clients or mods that", "give an unfair advantage."),
    BAD_NAME(Material.NAME_TAG, "Bad Name",
        "Username that is offensive, hateful", "or otherwise inappropriate."),
    GUILD_NAME(Material.BANNER, "Guild Name / Tag",
        "Guild name or tag that is offensive", "or breaks the server rules."),
    CROSS_TEAMING(Material.COMPASS, "Cross-Teaming",
        "Teaming with other teams in modes", "where it is not allowed."),
    BAD_SKIN(Material.LEATHER, "Bad Skin / Bad Cape",
        "Skin or cape that is explicit,", "offensive or inappropriate."),
    STATS_BOOSTING(Material.TNT, "Stats Boosting",
        "Farming wins, kills or levels with", "other players to boost stats.");

    private final Material icon;
    private final String displayName;
    private final List<String> description;

    ReportCategory(Material icon, String displayName, String... description) {
        this.icon = icon;
        this.displayName = displayName;
        this.description = Collections.unmodifiableList(Arrays.asList(description));
    }

    public Material getIcon() { return icon; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }

    public static ReportCategory fromName(String name) {
        if (name == null) return null;
        try { return valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }
}
