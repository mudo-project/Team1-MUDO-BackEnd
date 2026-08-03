package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalDetailResponse(
        Long id,
        Long templateId,
        String templateName,
        String title,
        ApprovalContentType contentType,
        String text,
        List<ApprovalAttachmentResponse> attachments,
        Long creatorId,
        String creatorName,
        ApprovalStatus status,
        LocalDateTime createdAt,
        List<ApprovalLineResponse> lines
) {

    public static ApprovalDetailResponse from(ApprovalDetailView view) {
        List<ApprovalLineResponse> lines = view.lines().stream()
                .map(ApprovalLineResponse::from)
                .toList();
        List<ApprovalAttachmentResponse> attachments = view.attachments().stream()
                .map(ApprovalAttachmentResponse::from)
                .toList();

        return new ApprovalDetailResponse(
                view.id(), view.templateId(), view.templateName(), view.title(), view.contentType(),
                view.text(), attachments, view.creatorId(), view.creatorName(),
                view.status(), view.createdAt(), lines);
    }
}
