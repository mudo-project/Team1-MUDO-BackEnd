package com.academy.mudogroupware.approval.application.command;

import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;

public record DecideApprovalLineCommand(
        Long documentId,
        Long approverId,
        ApprovalDecision decision,
        String comment
) {
}
