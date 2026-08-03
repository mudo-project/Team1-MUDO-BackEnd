package com.academy.mudogroupware.notice.application.usecase;

public interface PinNoticeUseCase {

    void pin(Long noticeId, Long requesterId);

    void unpin(Long noticeId, Long requesterId);
}
