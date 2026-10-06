package com.minestorm.reportsystem.model;

import java.util.UUID;

public final class Report {
    private final int id;
    private final UUID reporterUuid;
    private final String reporterName;
    private final UUID targetUuid;
    private final String targetName;
    private final ReportCategory category;
    private final long createdAt;

    public Report(int id, UUID reporterUuid, String reporterName,
                  UUID targetUuid, String targetName,
                  ReportCategory category, long createdAt) {
        this.id = id; this.reporterUuid = reporterUuid;
        this.reporterName = reporterName; this.targetUuid = targetUuid;
        this.targetName = targetName; this.category = category;
        this.createdAt = createdAt;
    }
    public int id() { return id; }
    public UUID reporterUuid() { return reporterUuid; }
    public String reporterName() { return reporterName; }
    public UUID targetUuid() { return targetUuid; }
    public String targetName() { return targetName; }
    public ReportCategory category() { return category; }
    public long createdAt() { return createdAt; }
}
