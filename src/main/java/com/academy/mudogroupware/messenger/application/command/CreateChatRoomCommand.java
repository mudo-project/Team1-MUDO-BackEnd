package com.academy.mudogroupware.messenger.application.command;

import java.util.List;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

// requesterId는 컨트롤러가 인증 주체(@AuthenticationPrincipal)에서 채워야 하며, 클라이언트 요청 바디 값을 그대로 신뢰해서는 안 된다.
public record CreateChatRoomCommand(
        Long requesterId,
        List<Long> participantIds,
        String name
) {
    public CreateChatRoomCommand {
        participantIds = validateParticipantIds(participantIds);
    }

    private static List<Long> validateParticipantIds(List<Long> participantIds) {
        if (participantIds == null) {
            return List.of();
        }
        if (participantIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new MessengerException(MessengerErrorCode.INVALID_PARTICIPANT);
        }
        return List.copyOf(participantIds);
    }
}
