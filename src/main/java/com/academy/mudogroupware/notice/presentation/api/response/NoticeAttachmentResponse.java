package com.academy.mudogroupware.notice.presentation.api.response;

import com.academy.mudogroupware.notice.application.query.NoticeAttachmentView;

public record NoticeAttachmentResponse(
        Long id,
        String fileUrl,
        String fileName,
        String fileType
) {

    public static NoticeAttachmentResponse from(NoticeAttachmentView view) {
        return new NoticeAttachmentResponse(view.id(), view.fileUrl(), view.fileName(), view.fileType());
    }
}
