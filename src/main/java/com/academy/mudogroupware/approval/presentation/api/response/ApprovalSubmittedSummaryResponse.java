package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalSubmittedSummaryResponse(
        Long id,
        String title,
        String templateName,
        String creatorName,
        ApprovalStatus status,
        Integer currentApproverStepOrder,
        String currentApproverName,
        LocalDateTime createdAt
) {

    public static ApprovalSubmittedSummaryResponse from(ApprovalSubmittedSummaryView view) {
        return new ApprovalSubmittedSummaryResponse(
                view.id(), view.title(), view.templateName(), view.creatorName(), view.status(),
                view.currentApproverStepOrder(), view.currentApproverName(), view.createdAt());
    }
}
