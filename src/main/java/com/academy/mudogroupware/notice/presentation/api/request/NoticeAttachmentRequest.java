package com.academy.mudogroupware.notice.presentation.api.request;

import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;

import jakarta.validation.constraints.NotBlank;

public record NoticeAttachmentRequest(
        @NotBlank String fileUrl,
        @NotBlank String fileName,
        String fileType
) {

    public NoticeAttachmentInput toInput() {
        return new NoticeAttachmentInput(fileUrl, fileName, fileType);
    }
}
