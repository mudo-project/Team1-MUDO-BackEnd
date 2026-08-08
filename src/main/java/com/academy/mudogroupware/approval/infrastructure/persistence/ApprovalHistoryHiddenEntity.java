package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_history_hidden",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_approval_history_hidden_document_user",
                columnNames = {"approval_document_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalHistoryHiddenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_history_hidden_id")
    private Long id;

    @Column(name = "approval_document_id", nullable = false)
    private Long approvalDocumentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hidden_at", nullable = false)
    private LocalDateTime hiddenAt;

    @Builder
    private ApprovalHistoryHiddenEntity(Long id, Long approvalDocumentId, Long userId, LocalDateTime hiddenAt) {
        this.id = id;
        this.approvalDocumentId = approvalDocumentId;
        this.userId = userId;
        this.hiddenAt = hiddenAt;
    }
}
