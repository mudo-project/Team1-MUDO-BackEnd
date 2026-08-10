package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskCardView(
        Long id,
        Long chatRoomId,
        Long assignerId,
        String assignerName,
        String content,
        LocalDate dueDate,
        List<TaskAssigneeView> assignees,
        long completedCount,
        int assigneeCount,
        boolean fullyCompleted,
        LocalDateTime createdAt
) {
}
