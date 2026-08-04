package com.academy.mudogroupware.messenger.application.query;

import java.util.List;

public record ChatMessagePageView(
        List<ChatMessageView> messages,
        boolean hasNext
) {
}
