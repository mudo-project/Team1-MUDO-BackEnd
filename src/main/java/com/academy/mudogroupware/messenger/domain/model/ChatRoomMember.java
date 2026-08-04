package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDateTime;

public final class ChatRoomMember {

    private final Long userId;
    private LocalDateTime lastReadAt;

    private ChatRoomMember(Long userId, LocalDateTime lastReadAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        this.userId = userId;
        this.lastReadAt = lastReadAt;
    }

    public static ChatRoomMember create(Long userId) {
        return new ChatRoomMember(userId, null);
    }

    public static ChatRoomMember restore(Long userId, LocalDateTime lastReadAt) {
        return new ChatRoomMember(userId, lastReadAt);
    }

    void markRead(LocalDateTime readAt) {
        if (readAt == null) {
            throw new IllegalArgumentException("readAt must not be null");
        }
        if (lastReadAt == null || readAt.isAfter(lastReadAt)) {
            this.lastReadAt = readAt;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }
}
