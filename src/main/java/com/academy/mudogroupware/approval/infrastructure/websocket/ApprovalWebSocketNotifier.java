package com.academy.mudogroupware.approval.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApprovalLineActivatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/approvals/users/" + event.approverId(),
                ApprovalLineActivatedSocketResponse.from(event));
    }
}
