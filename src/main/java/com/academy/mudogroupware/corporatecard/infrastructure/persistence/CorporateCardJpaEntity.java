package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.corporatecard.domain.model.CorporateCardStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "corporate_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateCardJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id") private Long id;
    @Column(name = "card_name", nullable = false, length = 100) private String cardName;
    @Column(name = "card_company", nullable = false, length = 100) private String cardCompany;
    @Column(name = "card_number_masked", nullable = false, length = 30) private String cardNumberMasked;
    @Column(name = "approval_template_id") private Long approvalTemplateId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CorporateCardStatus status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
