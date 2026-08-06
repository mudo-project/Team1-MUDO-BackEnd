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
import com.academy.mudogroupware.messenger.domain.event.MessageDeletedEvent;
import com.academy.mudogroupware.messenger.domain.event.MessageEditedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardDeletedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardUpdatedEvent;
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

    @Test
    void sendsMessageEditedEventToRoomTopic() {
        LocalDateTime editedAt = LocalDateTime.of(2026, 8, 6, 14, 0);
        MessageEditedEvent event = new MessageEditedEvent(1L, 5L, 2L, "after", editedAt);

        notifier.handle(event);

        ArgumentCaptor<MessageEditedSocketResponse> captor =
                ArgumentCaptor.forClass(MessageEditedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("MESSAGE_EDITED");
        assertThat(captor.getValue().messageId()).isEqualTo(5L);
        assertThat(captor.getValue().content()).isEqualTo("after");
    }

    @Test
    void sendsMessageDeletedEventToRoomTopic() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 6, 14, 5);
        MessageDeletedEvent event = new MessageDeletedEvent(1L, 5L, 2L, deletedAt);

        notifier.handle(event);

        ArgumentCaptor<MessageDeletedSocketResponse> captor =
                ArgumentCaptor.forClass(MessageDeletedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("MESSAGE_DELETED");
        assertThat(captor.getValue().messageId()).isEqualTo(5L);
        assertThat(captor.getValue().deleterUserId()).isEqualTo(2L);
    }

    @Test
    void sendsTaskCardUpdatedEventToRoomTopic() {
        TaskCardUpdatedEvent event = new TaskCardUpdatedEvent(
                1L, 7L, "new content", LocalDate.of(2026, 8, 20), List.of(4L, 5L));

        notifier.handle(event);

        ArgumentCaptor<TaskCardUpdatedSocketResponse> captor =
                ArgumentCaptor.forClass(TaskCardUpdatedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_CARD_UPDATED");
        assertThat(captor.getValue().cardId()).isEqualTo(7L);
        assertThat(captor.getValue().content()).isEqualTo("new content");
        assertThat(captor.getValue().assigneeIds()).containsExactly(4L, 5L);
    }

    @Test
    void sendsTaskCardDeletedEventToRoomTopic() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 6, 15, 0);
        TaskCardDeletedEvent event = new TaskCardDeletedEvent(1L, 7L, deletedAt);

        notifier.handle(event);

        ArgumentCaptor<TaskCardDeletedSocketResponse> captor =
                ArgumentCaptor.forClass(TaskCardDeletedSocketResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messenger/rooms/1"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_CARD_DELETED");
        assertThat(captor.getValue().cardId()).isEqualTo(7L);
        assertThat(captor.getValue().deletedAt()).isEqualTo(deletedAt);
    }
}
