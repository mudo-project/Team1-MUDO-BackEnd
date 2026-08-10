package com.academy.mudogroupware.notice.application.command;

import java.util.List;

public record CreateNoticeCommand(
        Long authorUserId,
        String title,
        String content,
        boolean pinned,
        List<NoticeAttachmentInput> attachments
) {
}
