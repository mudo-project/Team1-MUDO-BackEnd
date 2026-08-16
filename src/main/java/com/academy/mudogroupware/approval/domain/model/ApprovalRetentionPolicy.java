package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;

public enum ApprovalRetentionPolicy {
    GENERAL_BUSINESS(3),
    TAX_EVIDENCE(5),
    IMPORTANT_BUSINESS(10);

    private final int retentionYears;

    ApprovalRetentionPolicy(int retentionYears) {
        this.retentionYears = retentionYears;
    }

    public LocalDateTime calculateRetentionUntil(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        return createdAt.plusYears(retentionYears);
    }

    public static ApprovalRetentionPolicy fromSourceType(ApprovalDocumentSourceType sourceType) {
        if (sourceType == ApprovalDocumentSourceType.CORPORATE_CARD_EXPENSE) {
            return TAX_EVIDENCE;
        }
        return GENERAL_BUSINESS;
    }
}
