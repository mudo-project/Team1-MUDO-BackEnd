package com.academy.mudogroupware.notice.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.notice.application.command.CreateNoticeCommand;
import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;

import jakarta.validation.constraints.NotBlank;

public record CreateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        boolean pinned,
        List<NoticeAttachmentRequest> attachments
) {

    public CreateNoticeCommand toCommand(Long authorUserId) {
        List<NoticeAttachmentInput> attachmentInputs =
                attachments == null ? null : attachments.stream().map(NoticeAttachmentRequest::toInput).toList();
        return new CreateNoticeCommand(authorUserId, title, content, pinned, attachmentInputs);
    }
}
