package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.notice.domain.repository.NoticeReadRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NoticeReadRepositoryImpl implements NoticeReadRepository {

    private final NoticeReadJpaRepository noticeReadJpaRepository;

    @Override
    public void markRead(Long noticeId, Long userId) {
        if (noticeReadJpaRepository.existsByNoticeIdAndUserId(noticeId, userId)) {
            return;
        }
        noticeReadJpaRepository.save(NoticeReadEntity.builder()
                .noticeId(noticeId)
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build());
    }

    @Override
    public boolean hasRead(Long noticeId, Long userId) {
        return noticeReadJpaRepository.existsByNoticeIdAndUserId(noticeId, userId);
    }

    @Override
    public long countReaders(Long noticeId) {
        return noticeReadJpaRepository.countByNoticeId(noticeId);
    }

    @Override
    public Map<Long, LocalDateTime> findReadTimestamps(Long noticeId) {
        return noticeReadJpaRepository.findAllByNoticeId(noticeId).stream()
                .collect(Collectors.toMap(NoticeReadEntity::getUserId, NoticeReadEntity::getReadAt, (a, b) -> a));
    }
}
