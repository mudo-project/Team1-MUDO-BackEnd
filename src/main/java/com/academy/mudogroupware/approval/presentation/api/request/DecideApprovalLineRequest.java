package com.academy.mudogroupware.approval.presentation.api.request;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;

import jakarta.validation.constraints.NotNull;

public record DecideApprovalLineRequest(
        @NotNull ApprovalDecision decision,
        String comment
) {

    public DecideApprovalLineCommand toCommand(Long documentId, Long approverId) {
        return new DecideApprovalLineCommand(documentId, approverId, decision, comment);
    }
}
