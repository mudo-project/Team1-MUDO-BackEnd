package com.academy.mudogroupware.messenger.presentation.api.response;

public record ChatRoomCreateResponse(
        Long chatRoomId
) {

    public static ChatRoomCreateResponse from(Long chatRoomId) {
        return new ChatRoomCreateResponse(chatRoomId);
    }
}
