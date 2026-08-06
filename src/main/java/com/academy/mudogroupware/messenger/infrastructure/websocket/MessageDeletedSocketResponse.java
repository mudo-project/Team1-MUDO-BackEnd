package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.MessageDeletedEvent;

public record MessageDeletedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long messageId,
        Long deleterUserId,
        LocalDateTime deletedAt
) {

    public static MessageDeletedSocketResponse from(MessageDeletedEvent event) {
        return new MessageDeletedSocketResponse("MESSAGE_DELETED", event.chatRoomId(), event.messageId(),
                event.deleterUserId(), event.deletedAt());
    }
}
