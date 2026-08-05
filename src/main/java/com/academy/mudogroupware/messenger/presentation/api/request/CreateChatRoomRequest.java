package com.academy.mudogroupware.messenger.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.messenger.application.command.CreateChatRoomCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateChatRoomRequest(
        @NotEmpty List<@NotNull @Positive Long> participantIds,
        String name
) {

    public CreateChatRoomCommand toCommand(Long requesterId) {
        return new CreateChatRoomCommand(requesterId, participantIds, name);
    }
}
