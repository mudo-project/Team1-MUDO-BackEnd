package com.academy.mudogroupware.workspace.domain.event;

import java.time.LocalDateTime;

public record CommentCreatedEvent(
    Long workspaceId,
    Long taskId,
    Long commentId,
    Long authorId,
    String content,
    LocalDateTime createdAt) {}
