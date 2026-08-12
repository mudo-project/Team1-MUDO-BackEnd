package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findAllByPaidAtBetween(LocalDateTime from, LocalDateTime to) {
        return paymentJpaRepository.findAllByPaidAtBetween(from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    private Payment toDomain(PaymentEntity entity) {
        return Payment.restore(entity.getId(), entity.getEnrollmentId(), entity.getAmount(), entity.getPaidAt(),
                entity.getMethod(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
