package com.academy.mudogroupware.messenger.application.command;

import java.time.LocalDate;
import java.util.List;

public record CreateTaskCardCommand(
        Long chatRoomId,
        Long assignerId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds
) {
    public CreateTaskCardCommand {
        assigneeIds = assigneeIds == null ? List.of() : List.copyOf(assigneeIds);
    }
}
