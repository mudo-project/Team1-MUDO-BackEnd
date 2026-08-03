package com.academy.mudogroupware.notice.application.query;

import java.time.LocalDateTime;

public record NoticeReaderView(
        Long userId,
        String name,
        String role,
        LocalDateTime readAt
) {
}
