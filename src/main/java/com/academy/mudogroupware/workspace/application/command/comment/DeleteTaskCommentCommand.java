package com.academy.mudogroupware.workspace.application.command.comment;

public record DeleteTaskCommentCommand(Long workspaceId, Long taskId, Long commentId, Long requesterId) {}
