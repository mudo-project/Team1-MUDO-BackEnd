package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;

public record ApprovalLineView(
        Long lineId,
        int stepOrder,
        Long approverId,
        String approverName,
        ApprovalLineStatus status,
        String comment,
        LocalDateTime decidedAt
) {
}
