package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.event.TaskCardDeletedEvent;

public record TaskCardDeletedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long cardId,
        LocalDateTime deletedAt
) {

    public static TaskCardDeletedSocketResponse from(TaskCardDeletedEvent event) {
        return new TaskCardDeletedSocketResponse("TASK_CARD_DELETED", event.chatRoomId(), event.cardId(),
                event.deletedAt());
    }
}
