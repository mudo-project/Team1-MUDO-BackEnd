package com.academy.mudogroupware.notice.application.usecase;

public interface DeleteNoticeUseCase {

    void deleteNotice(Long noticeId, Long requesterId);
}
