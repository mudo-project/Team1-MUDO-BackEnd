package com.academy.mudogroupware.messenger.presentation.api.request;

import com.academy.mudogroupware.messenger.application.command.SendMessageCommand;
import com.academy.mudogroupware.messenger.domain.model.MessageType;

import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotNull MessageType messageType,
        String content,
        String fileUrl,
        String fileName
) {

    public SendMessageCommand toCommand(Long chatRoomId, Long senderId) {
        return new SendMessageCommand(chatRoomId, senderId, messageType, content, fileUrl, fileName);
    }
}
