package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

public record MessageEditedEvent(
        Long chatRoomId,
        Long messageId,
        Long senderUserId,
        String content,
        LocalDateTime editedAt
) {
}
