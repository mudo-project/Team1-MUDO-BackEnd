package com.academy.mudogroupware.workspace.application.command.workspace;

public record RemoveWorkspaceMemberCommand(
    // 요청자 id
    Long requesterId,
    // 워크스페이스 id
    Long workspaceId,
    // 제거 대상 id (요청자 자신이면 자진 탈퇴)
    Long targetUserId
) {}
