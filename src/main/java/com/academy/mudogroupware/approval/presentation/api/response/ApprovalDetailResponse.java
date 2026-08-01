package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalDetailResponse(
        Long id,
        String title,
        ApprovalContentType contentType,
        String text,
        String fileUrl,
        Long creatorId,
        ApprovalStatus status,
        LocalDateTime createdAt,
        List<ApprovalLineResponse> lines
) {

    public static ApprovalDetailResponse from(ApprovalDetailView view) {
        List<ApprovalLineResponse> lines = view.lines().stream()
                .map(ApprovalLineResponse::from)
                .toList();

        return new ApprovalDetailResponse(
                view.id(), view.title(), view.contentType(), view.text(), view.fileUrl(),
                view.creatorId(), view.status(), view.createdAt(), lines);
    }
}
