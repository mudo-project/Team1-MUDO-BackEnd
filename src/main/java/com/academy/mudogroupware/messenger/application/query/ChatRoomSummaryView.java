package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;

public record ChatRoomSummaryView(
        Long id,
        String name,
        ChatRoomType type,
        long unreadCount,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
}
