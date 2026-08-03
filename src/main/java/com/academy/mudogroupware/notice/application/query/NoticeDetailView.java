package com.academy.mudogroupware.notice.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailView(
        Long id,
        String title,
        String content,
        Long authorUserId,
        String authorName,
        String authorRole,
        boolean pinned,
        long viewCount,
        long readerCount,
        long totalRecipientCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NoticeAttachmentView> attachments
) {
}
