package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.model.MessageType;

public record ChatMessageSocketResponse(
        String eventType,
        Long chatRoomId,
        Long messageId,
        Long senderUserId,
        MessageType messageType,
        String content,
        Long fileId,
        String fileName,
        LocalDateTime createdAt,
        long unreadCount
) {

    public static ChatMessageSocketResponse from(ChatMessageSentEvent event) {
        return new ChatMessageSocketResponse("MESSAGE_SENT", event.chatRoomId(), event.messageId(),
                event.senderUserId(), event.messageType(), event.content(), event.fileId(), event.fileName(),
                event.createdAt(), event.unreadCount());
    }
}
