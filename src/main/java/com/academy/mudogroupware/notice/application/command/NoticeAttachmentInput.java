package com.academy.mudogroupware.notice.application.command;

public record NoticeAttachmentInput(
        Long fileId,
        String fileName
) {
}
