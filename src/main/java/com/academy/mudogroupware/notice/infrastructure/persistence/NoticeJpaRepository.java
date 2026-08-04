package com.academy.mudogroupware.notice.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeJpaRepository extends JpaRepository<NoticeEntity, Long> {

    @Query("select n from NoticeEntity n "
            + "where n.academyId = :academyId "
            + "and (:keyword is null or n.title like concat('%', :keyword, '%')) "
            + "order by n.pinned desc, n.createdAt desc")
    Slice<NoticeEntity> findAllByAcademyIdAndTitleKeyword(@Param("academyId") Long academyId,
                                                           @Param("keyword") String keyword, Pageable pageable);
}
