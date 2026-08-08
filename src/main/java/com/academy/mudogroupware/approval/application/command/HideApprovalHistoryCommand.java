package com.academy.mudogroupware.approval.application.command;

public record HideApprovalHistoryCommand(Long documentId, Long requesterId) {
}
