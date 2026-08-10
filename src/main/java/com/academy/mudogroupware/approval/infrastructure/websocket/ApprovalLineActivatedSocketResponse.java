package com.academy.mudogroupware.approval.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;

public record ApprovalLineActivatedSocketResponse(
        String eventType,
        Long documentId,
        String documentTitle,
        Long approverId,
        LocalDateTime activatedAt
) {

    public static ApprovalLineActivatedSocketResponse from(ApprovalLineActivatedEvent event) {
        return new ApprovalLineActivatedSocketResponse("APPROVAL_LINE_ACTIVATED", event.documentId(),
                event.documentTitle(), event.approverId(), event.activatedAt());
    }
}
