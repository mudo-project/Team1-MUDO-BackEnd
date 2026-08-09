package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "corporate_card_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateCardTransactionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id") private CorporateCardJpaEntity card;
    @Column(name = "approved_at", nullable = false) private LocalDateTime approvedAt;
    @Column(name = "approval_number", nullable = false, length = 50) private String approvalNumber;
    @Column(name = "merchant_name", nullable = false, length = 200) private String merchantName;
    @Column(name = "installment_months", nullable = false) private Integer installmentMonths;
    @Column(nullable = false) private Long amount;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
