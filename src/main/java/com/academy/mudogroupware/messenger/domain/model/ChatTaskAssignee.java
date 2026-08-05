package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDateTime;

public final class ChatTaskAssignee {

    private final Long userId;
    private LocalDateTime completedAt;

    private ChatTaskAssignee(Long userId, LocalDateTime completedAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        this.userId = userId;
        this.completedAt = completedAt;
    }

    public static ChatTaskAssignee create(Long userId) {
        return new ChatTaskAssignee(userId, null);
    }

    public static ChatTaskAssignee restore(Long userId, LocalDateTime completedAt) {
        return new ChatTaskAssignee(userId, completedAt);
    }

    void complete(LocalDateTime completedAt) {
        if (completedAt == null) {
            throw new IllegalArgumentException("completedAt must not be null");
        }
        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
