package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.ChatRoomMemberView;

public record ChatRoomMemberResponse(
        Long userId,
        String name,
        LocalDateTime lastReadAt
) {

    public static ChatRoomMemberResponse from(ChatRoomMemberView view) {
        return new ChatRoomMemberResponse(view.userId(), view.name(), view.lastReadAt());
    }
}
