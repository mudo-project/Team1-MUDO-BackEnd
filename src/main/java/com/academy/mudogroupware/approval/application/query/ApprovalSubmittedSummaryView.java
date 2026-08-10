package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;

public record ApprovalSubmittedSummaryView(
        Long id,
        ApprovalDocumentSourceType sourceType,
        String title,
        String templateName,
        String creatorName,
        ApprovalStatus status,
        Integer currentApproverStepOrder,
        String currentApproverName,
        LocalDateTime createdAt
) {
}
