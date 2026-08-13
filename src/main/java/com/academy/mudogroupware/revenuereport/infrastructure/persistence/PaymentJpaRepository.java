package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findAllByPaidAtGreaterThanEqualAndPaidAtLessThan(LocalDateTime from, LocalDateTime to);
}
