package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentMethod;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Builder
    private PaymentEntity(Long id, Long enrollmentId, Integer amount, LocalDateTime paidAt,
                          PaymentMethod method, PaymentStatus status) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.method = method;
        this.status = status;
    }
}
