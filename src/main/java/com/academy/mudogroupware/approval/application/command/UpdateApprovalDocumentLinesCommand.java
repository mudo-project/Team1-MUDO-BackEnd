package com.academy.mudogroupware.approval.application.command;

import java.util.List;

public record UpdateApprovalDocumentLinesCommand(
        Long documentId,
        Long requesterId,
        List<Long> approverIds
) {
}
