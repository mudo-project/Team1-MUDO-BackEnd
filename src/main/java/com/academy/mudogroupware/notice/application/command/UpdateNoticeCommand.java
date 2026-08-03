package com.academy.mudogroupware.notice.application.command;

public record UpdateNoticeCommand(
        Long noticeId,
        Long requesterId,
        String title,
        String content
) {
}
