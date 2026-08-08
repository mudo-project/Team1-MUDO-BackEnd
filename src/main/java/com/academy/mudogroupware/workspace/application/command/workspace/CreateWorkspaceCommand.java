package com.academy.mudogroupware.workspace.application.command.workspace;

import java.util.List;

public record CreateWorkspaceCommand(
        // 학원 id
        Long academyId,
        // 생성자 id
        Long creatorId,
        // 워크스페이스 이름
        String name,
        // 참여자
        List<Long> memberIds
) {}
