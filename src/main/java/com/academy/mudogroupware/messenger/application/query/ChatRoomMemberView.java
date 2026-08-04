package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;

public record ChatRoomMemberView(
        Long userId,
        String name,
        LocalDateTime lastReadAt
) {
}
