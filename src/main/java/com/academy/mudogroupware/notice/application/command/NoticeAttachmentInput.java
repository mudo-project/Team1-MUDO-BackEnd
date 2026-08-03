package com.academy.mudogroupware.notice.application.command;

public record NoticeAttachmentInput(
        String fileUrl,
        String fileName,
        String fileType
) {
}
