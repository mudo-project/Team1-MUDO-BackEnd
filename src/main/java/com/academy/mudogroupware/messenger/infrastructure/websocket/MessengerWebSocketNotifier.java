package com.academy.mudogroupware.messenger.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;

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
}
