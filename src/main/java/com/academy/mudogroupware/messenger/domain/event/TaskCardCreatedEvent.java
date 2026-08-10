package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskCardCreatedEvent(
        Long chatRoomId,
        Long cardId,
        Long assignerId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds,
        LocalDateTime createdAt
) {
}
