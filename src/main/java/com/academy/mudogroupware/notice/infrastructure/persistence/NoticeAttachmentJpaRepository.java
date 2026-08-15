package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.notice.application.retention.NoticeAttachmentFileReference;

public interface NoticeAttachmentJpaRepository extends JpaRepository<NoticeAttachmentEntity, Long> {

    @Query("""
            select new com.academy.mudogroupware.notice.application.retention.NoticeAttachmentFileReference(
                a.notice.id,
                a.fileId
            )
            from NoticeAttachmentEntity a
            where a.notice.id in :noticeIds
            order by a.notice.id asc, a.id asc
            """)
    List<NoticeAttachmentFileReference> findFileReferencesByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    @Modifying
    @Query("delete from NoticeAttachmentEntity a where a.notice.id in :noticeIds")
    int deleteAllByNoticeIds(@Param("noticeIds") List<Long> noticeIds);
}
