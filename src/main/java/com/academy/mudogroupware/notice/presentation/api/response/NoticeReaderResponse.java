package com.academy.mudogroupware.notice.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.notice.application.query.NoticeReaderView;

public record NoticeReaderResponse(
        Long userId,
        String name,
        String role,
        LocalDateTime readAt
) {

    public static NoticeReaderResponse from(NoticeReaderView view) {
        return new NoticeReaderResponse(view.userId(), view.name(), view.role(), view.readAt());
    }
}
