package com.academy.mudogroupware.approval.domain.model;

public final class ApprovalTemplateLine {

    private final Long id;
    private final int stepOrder;
    private final Long approverId;
    private final Long roleId;

    private ApprovalTemplateLine(Long id, int stepOrder, Long approverId, Long roleId) {
        if (approverId == null && roleId == null) {
            throw new IllegalArgumentException("approverId or roleId must be provided");
        }
        if (stepOrder < 1) {
            throw new IllegalArgumentException("stepOrder must be positive");
        }
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
        this.roleId = roleId;
    }

    public static ApprovalTemplateLine create(int stepOrder, Long approverId) {
        return new ApprovalTemplateLine(null, stepOrder, approverId, null);
    }

    public static ApprovalTemplateLine restore(Long id, int stepOrder, Long approverId, Long roleId) {
        return new ApprovalTemplateLine(id, stepOrder, approverId, roleId);
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

    public Long getRoleId() {
        return roleId;
    }
}
