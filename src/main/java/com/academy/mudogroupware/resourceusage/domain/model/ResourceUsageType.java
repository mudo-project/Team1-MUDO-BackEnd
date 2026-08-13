package com.academy.mudogroupware.resourceusage.domain.model;

public enum ResourceUsageType {
    AI_TOKEN("tokens"),
    SMS("messages");

    private final String unit;

    ResourceUsageType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
