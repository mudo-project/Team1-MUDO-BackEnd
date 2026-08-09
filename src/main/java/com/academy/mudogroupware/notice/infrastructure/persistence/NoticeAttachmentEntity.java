package com.academy.mudogroupware.notice.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private NoticeEntity notice;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Builder
    private NoticeAttachmentEntity(Long id, Long fileId, String fileName) {
        this.id = id;
        this.fileId = fileId;
        this.fileName = fileName;
    }

    void assignNotice(NoticeEntity notice) {
        this.notice = notice;
    }
}
