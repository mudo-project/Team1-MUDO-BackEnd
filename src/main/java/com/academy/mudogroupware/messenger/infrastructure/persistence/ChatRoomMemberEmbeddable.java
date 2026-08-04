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
public class ChatRoomMemberEmbeddable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Builder
    private ChatRoomMemberEmbeddable(Long userId, LocalDateTime lastReadAt) {
        this.userId = userId;
        this.lastReadAt = lastReadAt;
    }
}
