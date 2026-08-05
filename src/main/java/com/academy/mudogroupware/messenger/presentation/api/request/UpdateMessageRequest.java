package com.academy.mudogroupware.messenger.presentation.api.request;

import com.academy.mudogroupware.messenger.application.command.UpdateMessageCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageRequest(
        @NotBlank String content
) {

    public UpdateMessageCommand toCommand(Long chatRoomId, Long messageId, Long requesterId) {
        return new UpdateMessageCommand(chatRoomId, messageId, requesterId, content);
    }
}
