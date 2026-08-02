package com.academy.mudogroupware.approval.domain.model;

public final class ApprovalTemplateLine {

    private final Long id;
    private final int stepOrder;
    private final Long approverId;

    private ApprovalTemplateLine(Long id, int stepOrder, Long approverId) {
        if (approverId == null) {
            throw new IllegalArgumentException("approverId must not be null");
        }
        if (stepOrder < 1) {
            throw new IllegalArgumentException("stepOrder must be positive");
        }
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
    }

    public static ApprovalTemplateLine create(int stepOrder, Long approverId) {
        return new ApprovalTemplateLine(null, stepOrder, approverId);
    }

    public static ApprovalTemplateLine restore(Long id, int stepOrder, Long approverId) {
        return new ApprovalTemplateLine(id, stepOrder, approverId);
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
}
