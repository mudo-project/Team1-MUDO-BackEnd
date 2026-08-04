package com.academy.mudogroupware.messenger.application.command;

import java.util.List;

public record CreateChatRoomCommand(
        Long requesterId,
        List<Long> participantIds,
        String name
) {
}
