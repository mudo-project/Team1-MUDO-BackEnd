package com.academy.mudogroupware.revenuereport.domain.model;

import java.time.LocalDateTime;

public final class Payment {

    private final Long id;
    private final Long enrollmentId;
    private final Integer amount;
    private final LocalDateTime paidAt;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Payment(Long id, Long enrollmentId, Integer amount, LocalDateTime paidAt, PaymentMethod method,
                    PaymentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.method = method;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment restore(Long id, Long enrollmentId, Integer amount, LocalDateTime paidAt,
                                  PaymentMethod method, PaymentStatus status, LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        return new Payment(id, enrollmentId, amount, paidAt, method, status, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public Integer getAmount() { return amount; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
