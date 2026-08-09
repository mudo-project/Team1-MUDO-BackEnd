package com.academy.mudogroupware.notice.presentation.api.request;

import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoticeAttachmentRequest(
        @NotNull Long fileId,
        @NotBlank String fileName
) {

    public NoticeAttachmentInput toInput() {
        return new NoticeAttachmentInput(fileId, fileName);
    }
}
