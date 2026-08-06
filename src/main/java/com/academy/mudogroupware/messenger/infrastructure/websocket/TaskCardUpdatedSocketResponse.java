package com.academy.mudogroupware.messenger.infrastructure.websocket;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.messenger.domain.event.TaskCardUpdatedEvent;

public record TaskCardUpdatedSocketResponse(
        String eventType,
        Long chatRoomId,
        Long cardId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds
) {

    public static TaskCardUpdatedSocketResponse from(TaskCardUpdatedEvent event) {
        return new TaskCardUpdatedSocketResponse("TASK_CARD_UPDATED", event.chatRoomId(), event.cardId(),
                event.content(), event.dueDate(), event.assigneeIds());
    }
}
