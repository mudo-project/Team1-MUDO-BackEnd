package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessagePageView(
        List<ChatMessageView> messages,
        boolean hasNext,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorMessageId
) {
    public ChatMessagePageView {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
