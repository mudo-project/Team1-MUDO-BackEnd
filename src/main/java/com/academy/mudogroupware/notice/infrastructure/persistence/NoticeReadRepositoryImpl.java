package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.notice.domain.repository.NoticeReadRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NoticeReadRepositoryImpl implements NoticeReadRepository {

    private final NoticeReadJpaRepository noticeReadJpaRepository;
    private final Clock clock;

    @Override
    public void markRead(Long noticeId, Long userId) {
        if (noticeReadJpaRepository.existsByNoticeIdAndUserId(noticeId, userId)) {
            return;
        }
        try {
            noticeReadJpaRepository.save(NoticeReadEntity.builder()
                    .noticeId(noticeId)
                    .userId(userId)
                    .readAt(LocalDateTime.now(clock))
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 다른 요청이 먼저 커밋되어 uk_notice_read_notice_user 위반이 났다면,
            // 읽음 처리 자체는 이미 됐으므로 무시한다.
        }
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
