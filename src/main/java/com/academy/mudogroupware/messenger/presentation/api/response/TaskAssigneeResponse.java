package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.TaskAssigneeView;

public record TaskAssigneeResponse(
        Long userId,
        String name,
        LocalDateTime completedAt
) {

    public static TaskAssigneeResponse from(TaskAssigneeView view) {
        return new TaskAssigneeResponse(view.userId(), view.name(), view.completedAt());
    }
}
