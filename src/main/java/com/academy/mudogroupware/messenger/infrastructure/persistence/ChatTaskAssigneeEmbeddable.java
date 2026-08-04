package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatTaskAssigneeEmbeddable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private ChatTaskAssigneeEmbeddable(Long userId, LocalDateTime completedAt) {
        this.userId = userId;
        this.completedAt = completedAt;
    }
}
