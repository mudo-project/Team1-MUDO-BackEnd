package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;

public record ApprovalTemplateSummaryResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {

    public static ApprovalTemplateSummaryResponse from(ApprovalTemplateSummaryView view) {
        return new ApprovalTemplateSummaryResponse(view.id(), view.name(), view.createdAt());
    }
}
