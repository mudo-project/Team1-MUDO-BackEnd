package com.academy.mudogroupware.revenuereport.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.revenuereport.domain.model.Payment;

public interface PaymentRepository {

    List<Payment> findAllByPaidAtBetween(LocalDateTime from, LocalDateTime to);
}
