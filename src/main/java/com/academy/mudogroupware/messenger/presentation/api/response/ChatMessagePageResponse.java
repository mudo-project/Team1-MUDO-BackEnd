package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;

public record ChatMessagePageResponse(
        List<ChatMessageResponse> content,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorMessageId
) {

    public static ChatMessagePageResponse from(ChatMessagePageView view) {
        return new ChatMessagePageResponse(view.messages().stream().map(ChatMessageResponse::from).toList(),
                view.hasNext(), view.nextCursorCreatedAt(), view.nextCursorMessageId());
    }
}
