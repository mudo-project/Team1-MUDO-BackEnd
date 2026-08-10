package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardExpenseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id") private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", unique = true)
    private CorporateCardTransactionJpaEntity transaction;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(name = "expense_category", nullable = false, length = 30)
    private ExpenseCategory expenseCategory;
    @Column(nullable = false, length = 255) private String purpose;
    @Column(name = "approval_document_id") private Long approvalDocumentId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public CardExpenseJpaEntity(CorporateCardTransactionJpaEntity transaction, Long userId,
                                ExpenseCategory category, String purpose, LocalDateTime now) {
        this.transaction = transaction;
        this.userId = userId;
        this.expenseCategory = category;
        this.purpose = purpose;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(ExpenseCategory category, String purpose, Long approvalDocumentId, LocalDateTime now) {
        this.expenseCategory = category;
        this.purpose = purpose;
        this.approvalDocumentId = approvalDocumentId;
        this.updatedAt = now;
    }

    public void assignApprovalDocumentId(Long id, LocalDateTime now) {
        this.approvalDocumentId = id;
        this.updatedAt = now;
    }
}
