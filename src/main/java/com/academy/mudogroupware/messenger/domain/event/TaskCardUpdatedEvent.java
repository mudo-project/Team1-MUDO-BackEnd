package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDate;
import java.util.List;

public record TaskCardUpdatedEvent(
        Long chatRoomId,
        Long cardId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds
) {
}
