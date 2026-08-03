package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeReadJpaRepository extends JpaRepository<NoticeReadEntity, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    long countByNoticeId(Long noticeId);

    List<NoticeReadEntity> findAllByNoticeId(Long noticeId);
}
