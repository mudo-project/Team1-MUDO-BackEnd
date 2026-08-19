package com.academy.mudogroupware.notice.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;
import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<NoticeAttachmentRequest> attachments
) {

    public UpdateNoticeCommand toCommand(Long noticeId, Long requesterId) {
        List<NoticeAttachmentInput> attachmentInputs =
                attachments == null ? null : attachments.stream().map(NoticeAttachmentRequest::toInput).toList();
        return new UpdateNoticeCommand(noticeId, requesterId, title, content, attachmentInputs);
    }
}
