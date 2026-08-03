package com.academy.mudogroupware.notice.domain.repository;

import java.time.LocalDateTime;
import java.util.Map;

public interface NoticeReadRepository {

    void markRead(Long noticeId, Long userId);

    boolean hasRead(Long noticeId, Long userId);

    long countReaders(Long noticeId);

    Map<Long, LocalDateTime> findReadTimestamps(Long noticeId);
}
