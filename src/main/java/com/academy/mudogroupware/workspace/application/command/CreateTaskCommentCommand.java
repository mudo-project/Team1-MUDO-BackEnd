package com.academy.mudogroupware.workspace.application.command;

import java.util.List;

public record CreateTaskCommentCommand(
    Long workspaceId, Long taskId, Long requesterId, String content, List<Long> mentionedUserIds) {}
