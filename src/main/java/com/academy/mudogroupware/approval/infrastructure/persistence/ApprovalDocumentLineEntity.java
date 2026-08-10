package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "approval_step")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalDocumentLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_step_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_document_id", nullable = false)
    private ApprovalDocumentEntity approvalDocument;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_user_id", nullable = false)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalLineStatus status;

    @Column(length = 1000)
    private String comment;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Builder
    private ApprovalDocumentLineEntity(Long id, int stepOrder, Long approverId, ApprovalLineStatus status,
                                        String comment, LocalDateTime decidedAt) {
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
        this.status = status;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    void assignDocument(ApprovalDocumentEntity approvalDocument) {
        this.approvalDocument = approvalDocument;
    }
}
