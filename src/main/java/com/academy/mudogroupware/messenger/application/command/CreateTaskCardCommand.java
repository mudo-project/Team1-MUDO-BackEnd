package com.academy.mudogroupware.messenger.application.command;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

public record CreateTaskCardCommand(
        Long chatRoomId,
        Long assignerId,
        String content,
        LocalDate dueDate,
        List<Long> assigneeIds
) {
    public CreateTaskCardCommand {
        assigneeIds = validateAssigneeIds(assigneeIds);
    }

    private static List<Long> validateAssigneeIds(List<Long> assigneeIds) {
        if (assigneeIds == null) {
            return List.of();
        }
        if (assigneeIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new MessengerException(MessengerErrorCode.INVALID_ASSIGNEE);
        }
        return List.copyOf(assigneeIds);
    }
}
