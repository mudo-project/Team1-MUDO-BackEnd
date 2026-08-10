package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.AttachmentSummaryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_document_id", nullable = false)
    private ApprovalDocumentEntity approvalDocument;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false, length = 20)
    private AttachmentSummaryStatus summaryStatus;

    @Column(name = "summarized_at")
    private LocalDateTime summarizedAt;

    @Builder
    private ApprovalAttachmentEntity(Long id, Long fileId, String aiSummary, AttachmentSummaryStatus summaryStatus,
                                      LocalDateTime summarizedAt) {
        this.id = id;
        this.fileId = fileId;
        this.aiSummary = aiSummary;
        this.summaryStatus = summaryStatus;
        this.summarizedAt = summarizedAt;
    }

    void assignDocument(ApprovalDocumentEntity approvalDocument) {
        this.approvalDocument = approvalDocument;
    }
}
