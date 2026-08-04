package com.academy.mudogroupware.messenger.application.command;

import java.util.List;

// requesterId는 컨트롤러가 인증 주체(@AuthenticationPrincipal)에서 채워야 하며, 클라이언트 요청 바디 값을 그대로 신뢰해서는 안 된다.
public record CreateChatRoomCommand(
        Long requesterId,
        List<Long> participantIds,
        String name
) {
    public CreateChatRoomCommand {
        participantIds = participantIds == null ? List.of() : List.copyOf(participantIds);
    }
}
