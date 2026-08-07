package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.messenger.application.query.TaskCardPageView;

public record TaskCardPageResponse(
        List<TaskCardResponse> content,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorCardId
) {

    public static TaskCardPageResponse from(TaskCardPageView view) {
        return new TaskCardPageResponse(view.taskCards().stream().map(TaskCardResponse::from).toList(),
                view.hasNext(), view.nextCursorCreatedAt(), view.nextCursorCardId());
    }
}
