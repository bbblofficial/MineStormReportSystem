package com.minestorm.reportsystem.model;

import java.util.UUID;

public final class ChatRecord {
    private final UUID uuid; private final String name;
    private final ChatChannel channel; private final String target;
    private final String message; private final long time;

    public ChatRecord(UUID u, String n, ChatChannel c, String t, String m, long ti) {
        this.uuid = u; this.name = n; this.channel = c;
        this.target = t; this.message = m; this.time = ti;
    }
    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public ChatChannel channel() { return channel; }
    public String target() { return target; }
    public String message() { return message; }
    public long time() { return time; }
}
