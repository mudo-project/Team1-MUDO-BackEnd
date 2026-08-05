package com.academy.mudogroupware.messenger.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;
import com.academy.mudogroupware.messenger.domain.model.MessageType;

class MessengerWebSocketNotifierTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final MessengerWebSocketNotifier notifier = new MessengerWebSocketNotifier(messagingTemplate);

    @Test
    void sendsMessageEventToRoomTopic() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 14, 30);
        ChatMessageSentEvent event = new ChatMessageSentEvent(
                1L, 5L, 2L, MessageType.TEXT, "hello", null, null, createdAt, 3L);

        notifier.handle(event);

        ArgumentCaptor<ChatMessageSocketResponse> captor = ArgumentCaptor.forClass(ChatMessageSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().messageId()).isEqualTo(5L);
        assertThat(captor.getValue().unreadCount()).isEqualTo(3L);
    }

    @Test
    void sendsReadEventToRoomTopic() {
        LocalDateTime readAt = LocalDateTime.of(2026, 8, 5, 14, 31);
        ChatRoomReadEvent event = new ChatRoomReadEvent(1L, 2L, readAt);

        notifier.handle(event);

        ArgumentCaptor<ChatRoomReadSocketResponse> captor =
                ArgumentCaptor.forClass(ChatRoomReadSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("MESSAGE_READ");
        assertThat(captor.getValue().readerUserId()).isEqualTo(2L);
        assertThat(captor.getValue().readAt()).isEqualTo(readAt);
    }
}
