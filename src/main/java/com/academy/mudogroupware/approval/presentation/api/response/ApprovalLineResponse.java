package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalLineView;
import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;

public record ApprovalLineResponse(
        Long lineId,
        int stepOrder,
        Long approverId,
        String approverName,
        ApprovalLineStatus status,
        String comment,
        LocalDateTime decidedAt
) {

    public static ApprovalLineResponse from(ApprovalLineView view) {
        return new ApprovalLineResponse(
                view.lineId(), view.stepOrder(), view.approverId(), view.approverName(),
                view.status(), view.comment(), view.decidedAt());
    }
}
