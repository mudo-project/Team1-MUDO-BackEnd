package com.academy.mudogroupware.notice.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.notice.application.query.NoticeSummaryView;

public record NoticeSummaryResponse(
        Long id,
        String title,
        String authorName,
        String authorRole,
        boolean pinned,
        boolean read,
        boolean hasAttachment,
        LocalDateTime createdAt
) {

    public static NoticeSummaryResponse from(NoticeSummaryView view) {
        return new NoticeSummaryResponse(view.id(), view.title(), view.authorName(), view.authorRole(),
                view.pinned(), view.read(), view.hasAttachment(), view.createdAt());
    }
}
