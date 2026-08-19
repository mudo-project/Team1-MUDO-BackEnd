package com.academy.mudogroupware.workspace.domain.event;

import java.time.LocalDateTime;

public record CommentToggledEvent(
    Long workspaceId,
    Long taskId,
    Long commentId,
    boolean completed,
    Long completedBy,
    LocalDateTime completedAt) {}
