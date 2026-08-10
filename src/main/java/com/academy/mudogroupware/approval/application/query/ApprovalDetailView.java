package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;

public record ApprovalDetailView(
        Long id,
        ApprovalDocumentSourceType sourceType,
        Long templateId,
        String templateName,
        String title,
        ApprovalContentType contentType,
        String text,
        List<ApprovalAttachmentView> attachments,
        Long creatorId,
        String creatorName,
        ApprovalStatus status,
        LocalDateTime createdAt,
        List<ApprovalLineView> lines
) {
}
