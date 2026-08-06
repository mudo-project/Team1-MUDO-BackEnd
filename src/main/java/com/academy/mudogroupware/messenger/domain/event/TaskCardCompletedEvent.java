package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

public record TaskCardCompletedEvent(
        Long chatRoomId,
        Long cardId,
        Long completedUserId,
        LocalDateTime completedAt,
        long completedCount,
        int assigneeCount,
        boolean fullyCompleted
) {
}
