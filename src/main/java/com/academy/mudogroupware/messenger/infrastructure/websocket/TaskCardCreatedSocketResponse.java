package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;

public record TaskCardCreatedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long cardId,
        Long assignerId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds,
        LocalDateTime createdAt
) {

    public static TaskCardCreatedSocketResponse from(TaskCardCreatedEvent event) {
        return new TaskCardCreatedSocketResponse("TASK_CARD_CREATED", event.chatRoomId(), event.cardId(),
                event.assignerId(), event.content(), event.dueDate(), event.assigneeIds(), event.createdAt());
    }
}
