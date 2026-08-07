package com.academy.mudogroupware.workspace.application.command;

public record DeleteTaskCommentCommand(Long workspaceId, Long taskId, Long commentId, Long requesterId) {}
