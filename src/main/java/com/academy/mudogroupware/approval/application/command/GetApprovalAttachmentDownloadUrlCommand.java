package com.academy.mudogroupware.approval.application.command;

public record GetApprovalAttachmentDownloadUrlCommand(Long documentId, Long fileId, Long requesterId) {
}
