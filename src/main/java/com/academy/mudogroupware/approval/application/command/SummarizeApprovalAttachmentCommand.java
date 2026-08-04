package com.academy.mudogroupware.approval.application.command;

public record SummarizeApprovalAttachmentCommand(Long documentId, Long fileId, Long requesterId) {
}
