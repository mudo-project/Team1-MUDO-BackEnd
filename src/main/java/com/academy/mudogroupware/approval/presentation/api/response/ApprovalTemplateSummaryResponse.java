package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;

public record ApprovalTemplateSummaryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        List<ApprovalTemplateLineResponse> lines
) {

    public static ApprovalTemplateSummaryResponse from(ApprovalTemplateSummaryView view) {
        List<ApprovalTemplateLineResponse> lines = view.lines().stream()
                .map(ApprovalTemplateLineResponse::from)
                .toList();
        return new ApprovalTemplateSummaryResponse(view.id(), view.name(), view.createdAt(), lines);
    }
}
