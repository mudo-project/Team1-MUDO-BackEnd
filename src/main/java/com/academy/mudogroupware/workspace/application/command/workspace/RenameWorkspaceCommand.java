package com.academy.mudogroupware.workspace.application.command.workspace;

public record RenameWorkspaceCommand(
    // 요청자 id
    Long requesterId,
    // 워크스페이스 id
    Long workspaceId,
    // 새 이름
    String name
) {}
