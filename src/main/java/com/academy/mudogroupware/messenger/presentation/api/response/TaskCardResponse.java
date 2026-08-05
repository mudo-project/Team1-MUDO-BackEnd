package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.messenger.application.query.TaskCardView;

public record TaskCardResponse(
        Long id,
        Long assignerId,
        String assignerName,
        String content,
        LocalDate dueDate,
        List<TaskAssigneeResponse> assignees,
        long completedCount,
        int assigneeCount,
        boolean fullyCompleted,
        LocalDateTime createdAt
) {

    public static TaskCardResponse from(TaskCardView view) {
        List<TaskAssigneeResponse> assignees = view.assignees().stream().map(TaskAssigneeResponse::from).toList();
        return new TaskCardResponse(view.id(), view.assignerId(), view.assignerName(), view.content(),
                view.dueDate(), assignees, view.completedCount(), view.assigneeCount(), view.fullyCompleted(),
                view.createdAt());
    }
}
