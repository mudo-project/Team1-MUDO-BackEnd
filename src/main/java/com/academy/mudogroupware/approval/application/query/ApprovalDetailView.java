package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalDetailView(
        Long id,
        String title,
        ApprovalContentType contentType,
        String text,
        String fileUrl,
        Long creatorId,
        ApprovalStatus status,
        LocalDateTime createdAt,
        List<ApprovalLineView> lines
) {
}
