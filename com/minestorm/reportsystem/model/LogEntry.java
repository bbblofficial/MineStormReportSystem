package com.minestorm.reportsystem.model;

public final class LogEntry {
    private final long timestamp;
    private final String title;
    private final String text;

    public LogEntry(long timestamp, String title, String text) {
        this.timestamp = timestamp; this.title = title; this.text = text;
    }
    public long timestamp() { return timestamp; }
    public String title() { return title; }
    public String text() { return text; }
}
