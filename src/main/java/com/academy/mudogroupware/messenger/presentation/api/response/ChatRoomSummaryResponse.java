package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.ChatRoomSummaryView;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;

public record ChatRoomSummaryResponse(
        Long id,
        String name,
        ChatRoomType type,
        long unreadCount,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {

    public static ChatRoomSummaryResponse from(ChatRoomSummaryView view) {
        return new ChatRoomSummaryResponse(view.id(), view.name(), view.type(), view.unreadCount(),
                view.lastMessagePreview(), view.lastMessageAt(), view.createdAt());
    }
}
