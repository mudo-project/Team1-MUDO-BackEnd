package com.academy.mudogroupware.workspace.domain.event;

import java.time.LocalDateTime;

public record CommentUpdatedEvent(
    Long workspaceId, Long taskId, Long commentId, String content, LocalDateTime updatedAt) {}
