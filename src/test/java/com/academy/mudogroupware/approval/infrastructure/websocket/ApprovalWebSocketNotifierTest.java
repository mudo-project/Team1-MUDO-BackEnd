package com.academy.mudogroupware.approval.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;

class ApprovalWebSocketNotifierTest {

    private final WebSocketEventPublisher eventPublisher = mock(WebSocketEventPublisher.class);
    private final ApprovalWebSocketNotifier notifier = new ApprovalWebSocketNotifier(eventPublisher);

    @Test
    void sendsActivatedLineEventToApproverTopic() {
        LocalDateTime activatedAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        ApprovalLineActivatedEvent event = new ApprovalLineActivatedEvent(1L, "휴가신청", 2L, activatedAt);

        notifier.handle(event);

        ArgumentCaptor<ApprovalLineActivatedSocketResponse> captor =
                ArgumentCaptor.forClass(ApprovalLineActivatedSocketResponse.class);
        verify(eventPublisher).publish(eq("/topic/approvals/users/2"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("APPROVAL_LINE_ACTIVATED");
        assertThat(captor.getValue().documentId()).isEqualTo(1L);
        assertThat(captor.getValue().approverId()).isEqualTo(2L);
    }
}
