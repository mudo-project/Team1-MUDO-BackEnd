package com.academy.mudogroupware.workspace.application.command.workspace;

public record RecoverWorkspaceCommand(
    // 요청자 id
    Long requesterId,
    // 워크스페이스 id
    Long workspaceId
) {}
