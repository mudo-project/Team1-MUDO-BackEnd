package com.academy.mudogroupware.workspace.application.command.comment;

public record ToggleTaskCommentCompleteCommand(
    Long workspaceId, Long taskId, Long commentId, Long requesterId) {}
