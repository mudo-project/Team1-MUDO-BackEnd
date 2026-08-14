package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeJpaRepository extends JpaRepository<NoticeEntity, Long> {

    @Query("select n from NoticeEntity n "
            + "where (:keyword is null or n.title like concat('%', :keyword, '%')) "
            + "and n.deletedAt is null "
            + "order by n.pinned desc, n.createdAt desc")
    Slice<NoticeEntity> findAllByTitleKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("select n from NoticeEntity n where n.id = :id and n.deletedAt is null")
    Optional<NoticeEntity> findActiveById(@Param("id") Long id);

    @Query(value = """
            select n.notice_id
            from notice n
            where n.deleted_at is not null
              and n.retention_until <= :now
            order by n.retention_until asc, n.notice_id asc
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findHardDeleteCandidateIds(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
            delete from NoticeEntity n
            where n.id in :noticeIds
              and n.deletedAt is not null
              and n.retentionUntil <= :now
            """)
    int hardDeleteExpiredByIds(
            @Param("noticeIds") List<Long> noticeIds,
            @Param("now") LocalDateTime now
    );
}
