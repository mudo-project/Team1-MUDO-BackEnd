package com.academy.mudogroupware.revenuereport.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void restoresAllFields() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 1);

        Payment payment = Payment.restore(1L, 100L, 300000, paidAt, PaymentMethod.CARD, PaymentStatus.PAID, now, now);

        assertThat(payment.getId()).isEqualTo(1L);
        assertThat(payment.getEnrollmentId()).isEqualTo(100L);
        assertThat(payment.getAmount()).isEqualTo(300000);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
