package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeReadJpaRepository extends JpaRepository<NoticeReadEntity, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    long countByNoticeId(Long noticeId);

    List<NoticeReadEntity> findAllByNoticeId(Long noticeId);

    @Query("""
            select r.noticeId
            from NoticeReadEntity r
            where r.noticeId in :noticeIds
              and r.userId = :userId
            """)
    List<Long> findReadNoticeIds(
            @Param("noticeIds") List<Long> noticeIds,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("delete from NoticeReadEntity r where r.noticeId in :noticeIds")
    int deleteAllByNoticeIds(@Param("noticeIds") List<Long> noticeIds);
}
