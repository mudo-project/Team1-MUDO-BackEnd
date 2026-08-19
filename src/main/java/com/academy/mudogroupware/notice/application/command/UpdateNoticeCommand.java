package com.academy.mudogroupware.notice.application.command;

import java.util.List;

public record UpdateNoticeCommand(
        Long noticeId,
        Long requesterId,
        String title,
        String content,
        List<NoticeAttachmentInput> attachments
) {
}
