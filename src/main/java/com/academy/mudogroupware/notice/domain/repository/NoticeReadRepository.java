package com.academy.mudogroupware.notice.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NoticeReadRepository {

    void markRead(Long noticeId, Long userId);

    boolean hasRead(Long noticeId, Long userId);

    Set<Long> findReadNoticeIds(List<Long> noticeIds, Long userId);

    long countReaders(Long noticeId);

    Map<Long, LocalDateTime> findReadTimestamps(Long noticeId);
}
