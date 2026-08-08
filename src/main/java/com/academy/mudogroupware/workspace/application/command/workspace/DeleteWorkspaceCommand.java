package com.academy.mudogroupware.workspace.application.command.workspace;

public record DeleteWorkspaceCommand(
    // 요청자 id
    Long requesterId,
    // 워크스페이스 id
    Long workspaceId
) {}
