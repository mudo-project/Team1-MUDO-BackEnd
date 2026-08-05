package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

public record ChatRoomReadEvent(
        Long chatRoomId,
        Long readerUserId,
        LocalDateTime readAt
) {
}
