package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public record ApprovalSubmittedSummaryView(
        Long id,
        String title,
        ApprovalStatus status,
        LocalDateTime createdAt
) {
}
