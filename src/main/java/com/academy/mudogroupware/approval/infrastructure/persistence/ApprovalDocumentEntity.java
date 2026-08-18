package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;
import com.academy.mudogroupware.approval.domain.model.ApprovalRetentionPolicy;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_document_id")
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ApprovalDocumentSourceType sourceType;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ApprovalContentType contentType;

    @Lob
    private String text;

    @Column(name = "requester_user_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resubmitted_at")
    private LocalDateTime resubmittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "retention_policy", nullable = false, length = 30)
    private ApprovalRetentionPolicy retentionPolicy;

    @Column(name = "retention_until", nullable = false)
    private LocalDateTime retentionUntil;

    @Column(name = "legal_hold", nullable = false)
    private boolean legalHold;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "approvalDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    @BatchSize(size = 100)
    private List<ApprovalDocumentLineEntity> lines = new ArrayList<>();

    @OneToMany(mappedBy = "approvalDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<ApprovalAttachmentEntity> attachments = new ArrayList<>();

    @Builder
    private ApprovalDocumentEntity(Long id, Long templateId, ApprovalDocumentSourceType sourceType, String title,
                                    ApprovalContentType contentType, String text, Long creatorId,
                                    ApprovalStatus status, LocalDateTime createdAt, LocalDateTime resubmittedAt,
                                    ApprovalRetentionPolicy retentionPolicy, LocalDateTime retentionUntil,
                                    Boolean legalHold, LocalDateTime archivedAt, Long version) {
        ApprovalRetentionPolicy resolvedRetentionPolicy = retentionPolicy == null
                ? ApprovalRetentionPolicy.fromSourceType(sourceType)
                : retentionPolicy;
        this.id = id;
        this.templateId = templateId;
        this.sourceType = sourceType;
        this.title = title;
        this.contentType = contentType;
        this.text = text;
        this.creatorId = creatorId;
        this.status = status;
        this.createdAt = createdAt;
        this.resubmittedAt = resubmittedAt;
        this.retentionPolicy = resolvedRetentionPolicy;
        this.retentionUntil = retentionUntil == null && createdAt != null
                ? resolvedRetentionPolicy.calculateRetentionUntil(createdAt)
                : retentionUntil;
        this.legalHold = Boolean.TRUE.equals(legalHold);
        this.archivedAt = archivedAt;
        this.version = version;
    }

    public void addLine(ApprovalDocumentLineEntity line) {
        lines.add(line);
        line.assignDocument(this);
    }

    public void clearLines() {
        lines.clear();
    }

    public void addAttachment(ApprovalAttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.assignDocument(this);
    }
}
