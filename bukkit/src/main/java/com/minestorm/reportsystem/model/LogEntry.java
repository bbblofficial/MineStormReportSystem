package com.minestorm.reportsystem.model;

public final class LogEntry {
    private final long timestamp; private final String title; private final String text;
    public LogEntry(long t, String ti, String tx) { timestamp = t; title = ti; text = tx; }
    public long timestamp() { return timestamp; }
    public String title() { return title; }
    public String text() { return text; }
}
