package com.academy.mudogroupware.messenger.application.command;

import com.academy.mudogroupware.messenger.domain.model.MessageType;

public record SendMessageCommand(
        Long chatRoomId,
        Long senderId,
        MessageType messageType,
        String content,
        String fileUrl,
        String fileName
) {
}
