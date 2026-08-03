package com.academy.mudogroupware.notice.application.query;

import java.time.LocalDateTime;

public record NoticeSummaryView(
        Long id,
        String title,
        String authorName,
        String authorRole,
        boolean pinned,
        boolean read,
        boolean hasAttachment,
        LocalDateTime createdAt
) {
}
