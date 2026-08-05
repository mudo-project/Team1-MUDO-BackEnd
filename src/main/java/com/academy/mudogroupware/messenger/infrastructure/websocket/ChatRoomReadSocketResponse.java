package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;

public record ChatRoomReadSocketResponse(
        String eventType,
        Long chatRoomId,
        Long readerUserId,
        LocalDateTime readAt
) {

    public static ChatRoomReadSocketResponse from(ChatRoomReadEvent event) {
        return new ChatRoomReadSocketResponse("MESSAGE_READ", event.chatRoomId(), event.readerUserId(),
                event.readAt());
    }
}
