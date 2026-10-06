package com.minestorm.reportsystem.common.model;

public enum ReportCategory {
    CHAT_ABUSE  ("Chat Abuse / Spamming"),
    CHEATING    ("Cheating / Hacking"),
    BAD_NAME    ("Bad Name"),
    GUILD_NAME  ("Guild Name / Tag"),
    CROSS_TEAMING("Cross-Teaming"),
    BAD_SKIN    ("Bad Skin / Cape"),
    STATS_BOOSTING("Stats Boosting");

    private final String display;
    ReportCategory(String display) { this.display = display; }
    public String getDisplayName() { return display; }
    public static ReportCategory fromName(String n) {
        if (n == null) return null;
        try { return valueOf(n.toUpperCase()); } catch (Exception e) { return null; }
    }
}
