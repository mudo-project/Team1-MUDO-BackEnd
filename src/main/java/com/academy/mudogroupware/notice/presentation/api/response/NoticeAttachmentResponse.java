package com.academy.mudogroupware.notice.presentation.api.response;

import com.academy.mudogroupware.notice.application.query.NoticeAttachmentView;

public record NoticeAttachmentResponse(
        Long id,
        Long fileId,
        String fileName
) {

    public static NoticeAttachmentResponse from(NoticeAttachmentView view) {
        return new NoticeAttachmentResponse(view.id(), view.fileId(), view.fileName());
    }
}
