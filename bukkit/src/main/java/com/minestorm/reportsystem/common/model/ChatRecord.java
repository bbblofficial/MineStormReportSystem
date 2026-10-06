package com.minestorm.reportsystem.common.model;

import java.util.UUID;

public final class ChatRecord {
    private final UUID uuid;
    private final String name;
    private final String channel;
    private final String target;
    private final String message;
    private final long time;

    public ChatRecord(UUID uuid, String name, String channel, String target, String message, long time) {
        this.uuid = uuid; this.name = name; this.channel = channel;
        this.target = target; this.message = message; this.time = time;
    }
    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public String channel() { return channel; }
    public String target() { return target; }
    public String message() { return message; }
    public long time() { return time; }
}
