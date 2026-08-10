package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.MessageEditedEvent;

public record MessageEditedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long messageId,
        Long senderUserId,
        String content,
        LocalDateTime editedAt
) {

    public static MessageEditedSocketResponse from(MessageEditedEvent event) {
        return new MessageEditedSocketResponse("MESSAGE_EDITED", event.chatRoomId(), event.messageId(),
                event.senderUserId(), event.content(), event.editedAt());
    }
}
