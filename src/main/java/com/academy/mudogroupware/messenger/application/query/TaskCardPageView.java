package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record TaskCardPageView(
        List<TaskCardView> taskCards,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorCardId
) {
    public TaskCardPageView {
        taskCards = taskCards == null ? List.of() : List.copyOf(taskCards);
    }
}
