package com.academy.mudogroupware.notice.presentation.api.request;

import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content
) {

    public UpdateNoticeCommand toCommand(Long noticeId, Long requesterId) {
        return new UpdateNoticeCommand(noticeId, requesterId, title, content);
    }
}
