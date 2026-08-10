package com.academy.mudogroupware.approval.infrastructure.websocket;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalWebSocketNotifier {

    private final WebSocketEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApprovalLineActivatedEvent event) {
        eventPublisher.publish(
                "/topic/approvals/users/" + event.approverId(),
                ApprovalLineActivatedSocketResponse.from(event));
    }
}
