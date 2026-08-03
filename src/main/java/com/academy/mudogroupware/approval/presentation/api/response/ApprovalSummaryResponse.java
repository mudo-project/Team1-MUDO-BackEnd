package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalSummaryResponse(
        Long id,
        String title,
        String templateName,
        String creatorName,
        ApprovalStatus status,
        int myStepOrder,
        ApprovalLineStatus myLineStatus,
        Integer currentApproverStepOrder,
        String currentApproverName,
        LocalDateTime createdAt
) {

    public static ApprovalSummaryResponse from(ApprovalSummaryView view) {
        return new ApprovalSummaryResponse(
                view.id(), view.title(), view.templateName(), view.creatorName(), view.status(),
                view.myStepOrder(), view.myLineStatus(),
                view.currentApproverStepOrder(), view.currentApproverName(), view.createdAt());
    }
}
