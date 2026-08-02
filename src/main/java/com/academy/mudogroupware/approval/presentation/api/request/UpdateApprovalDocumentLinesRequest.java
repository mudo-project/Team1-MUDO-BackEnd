package com.academy.mudogroupware.approval.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalDocumentLinesCommand;

import jakarta.validation.constraints.NotEmpty;

public record UpdateApprovalDocumentLinesRequest(
        @NotEmpty List<Long> approverIds
) {

    public UpdateApprovalDocumentLinesCommand toCommand(Long documentId, Long requesterId) {
        return new UpdateApprovalDocumentLinesCommand(documentId, requesterId, approverIds);
    }
}
