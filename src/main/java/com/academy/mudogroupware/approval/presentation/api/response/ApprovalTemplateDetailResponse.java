package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateDetailView;

public record ApprovalTemplateDetailResponse(
        Long id,
        String name,
        Long creatorId,
        LocalDateTime createdAt,
        List<ApprovalTemplateLineResponse> lines
) {

    public static ApprovalTemplateDetailResponse from(ApprovalTemplateDetailView view) {
        List<ApprovalTemplateLineResponse> lines = view.lines().stream()
                .map(ApprovalTemplateLineResponse::from)
                .toList();
        return new ApprovalTemplateDetailResponse(view.id(), view.name(), view.creatorId(), view.createdAt(), lines);
    }
}
