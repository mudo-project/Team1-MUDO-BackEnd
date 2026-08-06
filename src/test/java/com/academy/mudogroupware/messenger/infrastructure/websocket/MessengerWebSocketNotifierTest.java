package com.academy.mudogroupware.messenger.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;
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

    @Test
    void sendsTaskCardCreatedEventToRoomTopic() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 9, 0);
        TaskCardCreatedEvent event = new TaskCardCreatedEvent(
                1L, 7L, 2L, "과제 제출", LocalDate.of(2026, 8, 10), List.of(3L, 4L), createdAt);

        notifier.handle(event);

        ArgumentCaptor<TaskCardCreatedSocketResponse> captor =
                ArgumentCaptor.forClass(TaskCardCreatedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_CARD_CREATED");
        assertThat(captor.getValue().cardId()).isEqualTo(7L);
        assertThat(captor.getValue().assigneeIds()).containsExactly(3L, 4L);
    }

    @Test
    void sendsTaskCardCompletedEventToRoomTopic() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 6, 9, 30);
        TaskCardCompletedEvent event = new TaskCardCompletedEvent(1L, 7L, 3L, completedAt, 1L, 2, false);

        notifier.handle(event);

        ArgumentCaptor<TaskCardCompletedSocketResponse> captor =
                ArgumentCaptor.forClass(TaskCardCompletedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_CARD_COMPLETED");
        assertThat(captor.getValue().completedUserId()).isEqualTo(3L);
        assertThat(captor.getValue().completedCount()).isEqualTo(1L);
        assertThat(captor.getValue().assigneeCount()).isEqualTo(2);
        assertThat(captor.getValue().fullyCompleted()).isFalse();
    }

    @Test
    void sendsFullyCompletedTaskCardEventToRoomTopic() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 6, 9, 40);
        TaskCardCompletedEvent event = new TaskCardCompletedEvent(1L, 7L, 4L, completedAt, 2L, 2, true);

        notifier.handle(event);

        ArgumentCaptor<TaskCardCompletedSocketResponse> captor =
                ArgumentCaptor.forClass(TaskCardCompletedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().completedCount()).isEqualTo(2L);
        assertThat(captor.getValue().assigneeCount()).isEqualTo(2);
        assertThat(captor.getValue().fullyCompleted()).isTrue();
    }
}
