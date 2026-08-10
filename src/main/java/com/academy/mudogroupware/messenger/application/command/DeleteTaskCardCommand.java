package com.academy.mudogroupware.messenger.application.command;

public record DeleteTaskCardCommand(
        Long chatRoomId,
        Long cardId,
        Long requesterId
) {
}
