package com.minestorm.reportsystem.model;

public enum ReportOutcome {
    APPROVED("Approved (Valid)"),
    REJECTED("Rejected (False)"),
    INVALID_CATEGORY("Invalid Category");
    private final String label;
    ReportOutcome(String l) { label = l; }
    public String getLabel() { return label; }
}
