package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

public record MessageDeletedEvent(
        Long chatRoomId,
        Long messageId,
        Long deleterUserId,
        LocalDateTime deletedAt
) {
}
