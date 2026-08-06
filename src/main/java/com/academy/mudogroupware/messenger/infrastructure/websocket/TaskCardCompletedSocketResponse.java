package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;

public record TaskCardCompletedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long cardId,
        Long completedUserId,
        LocalDateTime completedAt,
        long completedCount,
        int assigneeCount,
        boolean fullyCompleted
) {

    public static TaskCardCompletedSocketResponse from(TaskCardCompletedEvent event) {
        return new TaskCardCompletedSocketResponse("TASK_CARD_COMPLETED", event.chatRoomId(), event.cardId(),
                event.completedUserId(), event.completedAt(), event.completedCount(), event.assigneeCount(),
                event.fullyCompleted());
    }
}
