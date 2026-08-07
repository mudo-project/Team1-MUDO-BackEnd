package com.academy.mudogroupware.workspace.application.command;

public record ToggleTaskCommentCompleteCommand(
    Long workspaceId, Long taskId, Long commentId, Long requesterId) {}
