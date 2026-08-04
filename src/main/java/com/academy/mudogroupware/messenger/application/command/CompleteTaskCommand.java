package com.academy.mudogroupware.messenger.application.command;

public record CompleteTaskCommand(
        Long chatRoomId,
        Long cardId,
        Long userId
) {
}
