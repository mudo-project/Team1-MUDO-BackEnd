package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

public record TaskCardDeletedEvent(
        Long chatRoomId,
        Long cardId,
        LocalDateTime deletedAt
) {
}
