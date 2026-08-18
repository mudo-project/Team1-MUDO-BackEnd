package com.academy.mudogroupware.planquota.domain.model;

public enum Plan {
    FREE("무료 플랜"),
    PAID("유료 플랜");

    private final String label;

    Plan(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
