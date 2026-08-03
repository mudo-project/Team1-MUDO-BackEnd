package com.academy.mudogroupware.notice.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.notice.application.query.NoticeDetailView;

public record NoticeDetailResponse(
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
        List<NoticeAttachmentResponse> attachments
) {

    public static NoticeDetailResponse from(NoticeDetailView view) {
        List<NoticeAttachmentResponse> attachments = view.attachments().stream()
                .map(NoticeAttachmentResponse::from)
                .toList();

        return new NoticeDetailResponse(
                view.id(), view.title(), view.content(), view.authorUserId(), view.authorName(), view.authorRole(),
                view.pinned(), view.viewCount(), view.readerCount(), view.totalRecipientCount(),
                view.createdAt(), view.updatedAt(), attachments);
    }
}
