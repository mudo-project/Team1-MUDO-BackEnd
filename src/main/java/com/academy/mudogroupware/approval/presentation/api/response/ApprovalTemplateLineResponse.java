package com.academy.mudogroupware.approval.presentation.api.response;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateLineView;

public record ApprovalTemplateLineResponse(
        int stepOrder,
        Long approverId,
        String approverName
) {

    public static ApprovalTemplateLineResponse from(ApprovalTemplateLineView view) {
        return new ApprovalTemplateLineResponse(view.stepOrder(), view.approverId(), view.approverName());
    }
}
