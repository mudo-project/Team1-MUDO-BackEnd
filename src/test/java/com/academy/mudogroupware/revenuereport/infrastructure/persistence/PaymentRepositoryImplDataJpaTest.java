package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentMethod;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentStatus;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, PaymentRepositoryImpl.class})
class PaymentRepositoryImplDataJpaTest {

    @org.springframework.beans.factory.annotation.Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PaymentRepositoryImpl paymentRepositoryImpl;

    @Test
    void findsPaymentsWithinRange() {
        PaymentEntity inRange = PaymentEntity.builder()
                .enrollmentId(1L).amount(300000)
                .paidAt(LocalDateTime.of(2026, 8, 10, 10, 0))
                .method(PaymentMethod.CARD).status(PaymentStatus.PAID)
                .build();
        PaymentEntity outOfRange = PaymentEntity.builder()
                .enrollmentId(2L).amount(200000)
                .paidAt(LocalDateTime.of(2026, 7, 10, 10, 0))
                .method(PaymentMethod.CASH).status(PaymentStatus.PAID)
                .build();
        paymentJpaRepository.save(inRange);
        paymentJpaRepository.save(outOfRange);

        java.util.List<Payment> result = paymentRepositoryImpl.findAllByPaidAtBetween(
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEnrollmentId()).isEqualTo(1L);
    }

    @Test
    void excludesPaymentExactlyAtExclusiveUpperBound() {
        PaymentEntity atLowerBound = PaymentEntity.builder()
                .enrollmentId(1L).amount(300000)
                .paidAt(LocalDateTime.of(2026, 8, 1, 0, 0))
                .method(PaymentMethod.CARD).status(PaymentStatus.PAID)
                .build();
        PaymentEntity atUpperBound = PaymentEntity.builder()
                .enrollmentId(2L).amount(200000)
                .paidAt(LocalDateTime.of(2026, 9, 1, 0, 0))
                .method(PaymentMethod.CASH).status(PaymentStatus.PAID)
                .build();
        paymentJpaRepository.save(atLowerBound);
        paymentJpaRepository.save(atUpperBound);

        java.util.List<Payment> result = paymentRepositoryImpl.findAllByPaidAtBetween(
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEnrollmentId()).isEqualTo(1L);
    }
}
