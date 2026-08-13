package com.academy.mudogroupware.notification.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    // createdAt 단독 정렬은 같은 시각에 생성된 알림의 순서를 보장하지 않아 id를 tiebreak로 추가한다.
    Slice<NotificationEntity> findAllByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndDeletedAtIsNullAndReadAtIsNull(Long recipientUserId);

    Optional<NotificationEntity> findByIdAndRecipientUserIdAndDeletedAtIsNull(Long id, Long recipientUserId);

    @Modifying
    @Query("update NotificationEntity n set n.deletedAt = :deletedAt "
            + "where n.recipientUserId = :recipientUserId and n.deletedAt is null and n.readAt is not null")
    int bulkSoftDeleteRead(@Param("recipientUserId") Long recipientUserId, @Param("deletedAt") LocalDateTime deletedAt);
}
