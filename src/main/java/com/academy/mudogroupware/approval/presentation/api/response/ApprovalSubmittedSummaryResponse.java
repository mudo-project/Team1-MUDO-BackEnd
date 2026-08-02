package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalSubmittedSummaryResponse(
        Long id,
        String title,
        ApprovalStatus status,
        LocalDateTime createdAt
) {

    public static ApprovalSubmittedSummaryResponse from(ApprovalSubmittedSummaryView view) {
        return new ApprovalSubmittedSummaryResponse(view.id(), view.title(), view.status(), view.createdAt());
    }
}
