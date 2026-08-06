package com.academy.mudogroupware.workspace.application.command;

import java.util.List;

public record AddWorkspaceMembersCommand(
    // 학원 id
    Long academyId,
    // 요청자 id
    Long requesterId,
    // 워크스페이스 id
    Long workspaceId,
    // 추가할 참여자 후보
    List<Long> memberIds
) {}
