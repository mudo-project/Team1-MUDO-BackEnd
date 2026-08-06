package com.academy.mudogroupware.messenger.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;
import com.academy.mudogroupware.messenger.domain.event.MessageDeletedEvent;
import com.academy.mudogroupware.messenger.domain.event.MessageEditedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardDeletedEvent;
import com.academy.mudogroupware.messenger.domain.event.TaskCardUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessengerWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                ChatMessageSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatRoomReadEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                ChatRoomReadSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TaskCardCreatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                TaskCardCreatedSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TaskCardCompletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                TaskCardCompletedSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MessageEditedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                MessageEditedSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MessageDeletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                MessageDeletedSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TaskCardUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                TaskCardUpdatedSocketResponse.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TaskCardDeletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/messenger/rooms/" + event.chatRoomId(),
                TaskCardDeletedSocketResponse.from(event));
    }
}
