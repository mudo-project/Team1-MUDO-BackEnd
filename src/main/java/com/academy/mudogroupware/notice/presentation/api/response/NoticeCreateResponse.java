package com.academy.mudogroupware.notice.presentation.api.response;

public record NoticeCreateResponse(
        Long noticeId
) {

    public static NoticeCreateResponse from(Long noticeId) {
        return new NoticeCreateResponse(noticeId);
    }
}
