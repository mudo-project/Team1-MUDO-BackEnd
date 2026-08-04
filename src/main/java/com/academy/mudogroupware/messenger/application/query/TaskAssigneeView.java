package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;

public record TaskAssigneeView(
        Long userId,
        String name,
        LocalDateTime completedAt
) {
}
