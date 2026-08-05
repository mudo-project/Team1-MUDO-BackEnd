package com.academy.mudogroupware.messenger.application.command;

public record UpdateMessageCommand(
        Long chatRoomId,
        Long messageId,
        Long requesterId,
        String content
) {
}
