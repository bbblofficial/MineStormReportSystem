package com.minestorm.reportsystem.common.model;

public enum ReportOutcome {
    APPROVED("Approved (Valid)"),
    REJECTED("Rejected (False Report)"),
    INVALID_CATEGORY("Invalid Category");

    private final String label;
    ReportOutcome(String label) { this.label = label; }
    public String getLabel() { return label; }
}
