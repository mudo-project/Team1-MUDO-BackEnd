package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice_read")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeReadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @Builder
    private NoticeReadEntity(Long noticeId, Long userId, LocalDateTime readAt) {
        this.noticeId = noticeId;
        this.userId = userId;
        this.readAt = readAt;
    }
}
