package com.academy.mudogroupware.workspace.application.command.comment;

import java.util.List;

public record UpdateTaskCommentCommand(
    Long workspaceId,
    Long taskId,
    Long commentId,
    Long requesterId,
    String content,
    List<Long> mentionedUserIds) {}
