package com.academy.mudogroupware.notice.application.query;

public record NoticeAttachmentView(
        Long id,
        String fileUrl,
        String fileName,
        String fileType
) {
}
