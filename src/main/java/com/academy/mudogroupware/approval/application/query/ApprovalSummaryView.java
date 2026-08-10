package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;

public record ApprovalSummaryView(
        Long id,
        ApprovalDocumentSourceType sourceType,
        String title,
        String templateName,
        String creatorName,
        ApprovalStatus status,
        int myStepOrder,
        ApprovalLineStatus myLineStatus,
        Integer currentApproverStepOrder,
        String currentApproverName,
        LocalDateTime createdAt
) {
}
