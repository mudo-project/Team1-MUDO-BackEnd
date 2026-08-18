package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Setter
    @Column(nullable = false, length = 200)
    private String title;

    @Setter
    @Lob
    @Column(nullable = false)
    private String content;

    @Setter
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Setter
    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Setter
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Setter
    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<NoticeAttachmentEntity> attachments = new ArrayList<>();

    @Builder
    private NoticeEntity(Long id, Long authorUserId, String title, String content, boolean pinned,
                          long viewCount, LocalDateTime createdAt, LocalDateTime updatedAt,
                          LocalDateTime deletedAt, LocalDateTime retentionUntil) {
        this.id = id;
        this.authorUserId = authorUserId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.retentionUntil = retentionUntil;
    }

    public void addAttachment(NoticeAttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.assignNotice(this);
    }
}
