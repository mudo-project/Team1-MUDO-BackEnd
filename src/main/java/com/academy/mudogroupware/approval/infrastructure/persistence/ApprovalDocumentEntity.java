package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
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

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

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

    @OneToMany(mappedBy = "approvalDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    private List<ApprovalDocumentLineEntity> lines = new ArrayList<>();

    @OneToMany(mappedBy = "approvalDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalAttachmentEntity> attachments = new ArrayList<>();

    @Builder
    private ApprovalDocumentEntity(Long id, Long academyId, Long templateId, String title,
                                    ApprovalContentType contentType, String text, Long creatorId,
                                    ApprovalStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.academyId = academyId;
        this.templateId = templateId;
        this.title = title;
        this.contentType = contentType;
        this.text = text;
        this.creatorId = creatorId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addLine(ApprovalDocumentLineEntity line) {
        lines.add(line);
        line.assignDocument(this);
    }

    public void addAttachment(ApprovalAttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.assignDocument(this);
    }
}
