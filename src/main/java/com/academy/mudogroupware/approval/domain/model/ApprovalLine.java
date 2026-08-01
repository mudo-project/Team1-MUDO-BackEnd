package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.error.BusinessException;

public final class ApprovalLine {

    private final Long id;
    private final int stepOrder;
    private final Long approverId;
    private ApprovalLineStatus status;
    private String comment;
    private LocalDateTime decidedAt;

    private ApprovalLine(Long id, int stepOrder, Long approverId, ApprovalLineStatus status,
                          String comment, LocalDateTime decidedAt) {
        if (approverId == null) {
            throw new IllegalArgumentException("approverId must not be null");
        }
        if (stepOrder < 1) {
            throw new IllegalArgumentException("stepOrder must be positive");
        }
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
        this.status = status;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    public static ApprovalLine create(int stepOrder, Long approverId) {
        ApprovalLineStatus initialStatus = stepOrder == 1 ? ApprovalLineStatus.PENDING : ApprovalLineStatus.WAITING;
        return new ApprovalLine(null, stepOrder, approverId, initialStatus, null, null);
    }

    public static ApprovalLine restore(Long id, int stepOrder, Long approverId, ApprovalLineStatus status,
                                        String comment, LocalDateTime decidedAt) {
        return new ApprovalLine(id, stepOrder, approverId, status, comment, decidedAt);
    }

    void activate() {
        this.status = ApprovalLineStatus.PENDING;
    }

    void approve(String comment) {
        ensurePending();
        this.status = ApprovalLineStatus.APPROVED;
        this.comment = comment;
        this.decidedAt = LocalDateTime.now();
    }

    void reject(String comment) {
        ensurePending();
        this.status = ApprovalLineStatus.REJECTED;
        this.comment = comment;
        this.decidedAt = LocalDateTime.now();
    }

    private void ensurePending() {
        if (status != ApprovalLineStatus.PENDING) {
            throw new BusinessException(ApprovalErrorCode.ALREADY_DECIDED);
        }
    }

    public Long getId() {
        return id;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public Long getApproverId() {
        return approverId;
    }

    public ApprovalLineStatus getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
