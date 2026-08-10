package com.academy.mudogroupware.messenger.application.command;

import com.academy.mudogroupware.messenger.domain.model.MessageType;

// senderId는 컨트롤러가 인증 주체(@AuthenticationPrincipal)에서 채워야 하며, 클라이언트 요청 바디 값을 그대로 신뢰해서는 안 된다.
public record SendMessageCommand(
        Long chatRoomId,
        Long senderId,
        MessageType messageType,
        String content,
        Long fileId,
        String fileName
) {
}
