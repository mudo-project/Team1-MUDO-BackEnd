package com.academy.mudogroupware.approval.application.command;

public record CancelApprovalDocumentCommand(Long documentId, Long requesterId) {
}
