package com.academy.mudogroupware.workspace.domain.event;

public record CommentDeletedEvent(Long workspaceId, Long taskId, Long commentId) {}
