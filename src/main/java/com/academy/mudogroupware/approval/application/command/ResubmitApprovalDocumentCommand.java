package com.academy.mudogroupware.approval.application.command;

public record ResubmitApprovalDocumentCommand(
        Long documentId,
        Long requesterId
) {
}
