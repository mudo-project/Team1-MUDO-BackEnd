package com.academy.mudogroupware.messenger.presentation.api.response;

public record MessageSendResponse(
        Long messageId
) {

    public static MessageSendResponse from(Long messageId) {
        return new MessageSendResponse(messageId);
    }
}
