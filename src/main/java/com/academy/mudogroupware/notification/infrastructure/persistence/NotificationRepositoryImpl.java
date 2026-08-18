package com.academy.mudogroupware.notification.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notification.domain.exception.NotificationErrorCode;
import com.academy.mudogroupware.notification.domain.exception.NotificationException;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationEntity.builder()
                .recipientUserId(notification.getRecipientUserId())
                .type(notification.getType())
                .targetId(notification.getTargetId())
                .message(notification.getMessage())
                .idempotencyKey(notification.getIdempotencyKey())
                .build();
        return toDomain(notificationJpaRepository.save(entity));
    }

    @Override
    public PageResult<Notification> findAllByRecipientUserId(Long recipientUserId, int page, int size) {
        Slice<NotificationEntity> slice = notificationJpaRepository
                .findAllByRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(recipientUserId, PageRequest.of(page, size));
        List<Notification> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public long countUnreadByRecipientUserId(Long recipientUserId) {
        return notificationJpaRepository.countByRecipientUserIdAndDeletedAtIsNullAndReadAtIsNull(recipientUserId);
    }

    @Override
    public void markAsRead(Long id, Long recipientUserId, LocalDateTime readAt) {
        NotificationEntity entity = findOwnedOrThrow(id, recipientUserId);
        entity.markAsRead(readAt);
    }

    @Override
    public void delete(Long id, Long recipientUserId, LocalDateTime deletedAt) {
        NotificationEntity entity = findOwnedOrThrow(id, recipientUserId);
        entity.markDeleted(deletedAt);
    }

    @Override
    public int deleteAllReadByRecipientUserId(Long recipientUserId, LocalDateTime deletedAt) {
        return notificationJpaRepository.bulkSoftDeleteRead(recipientUserId, deletedAt);
    }

    private NotificationEntity findOwnedOrThrow(Long id, Long recipientUserId) {
        return notificationJpaRepository.findByIdAndRecipientUserIdAndDeletedAtIsNull(id, recipientUserId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOT_FOUND));
    }

    private Notification toDomain(NotificationEntity entity) {
        return Notification.restore(entity.getId(), entity.getRecipientUserId(), entity.getType(),
                entity.getTargetId(), entity.getMessage(), entity.getIdempotencyKey(),
                entity.getReadAt(), entity.getCreatedAt());
    }
}
