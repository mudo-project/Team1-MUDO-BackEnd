package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * approval 모듈에서 승인된 휴가 기간을 attendance 쪽에서 자체적으로 들고 있기 위한 모델.
 * approval의 ApprovalDocument를 직접 참조하지 않고 documentId(식별자)만 보관한다 - approval이
 * 발행하는 LeaveRequestSubmittedEvent/ApprovalDocumentDecidedEvent를 소비해 생성·갱신된다.
 */
public final class LeaveRequest {

    private final Long id;
    private final Long academyId;
    private final Long userId;
    private final Long documentId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private LeaveRequestStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LeaveRequest(Long id, Long academyId, Long userId, Long documentId, LocalDate startDate,
                         LocalDate endDate, LeaveRequestStatus status, LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.documentId = documentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LeaveRequest submit(Long academyId, Long userId, Long documentId, LocalDate startDate,
                                      LocalDate endDate, LocalDateTime now) {
        return new LeaveRequest(null, academyId, userId, documentId, startDate, endDate,
                LeaveRequestStatus.PENDING, now, now);
    }

    public static LeaveRequest restore(Long id, Long academyId, Long userId, Long documentId, LocalDate startDate,
                                       LocalDate endDate, LeaveRequestStatus status, LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        return new LeaveRequest(id, academyId, userId, documentId, startDate, endDate, status, createdAt,
                updatedAt);
    }

    public void confirm(LocalDateTime now) {
        this.status = LeaveRequestStatus.CONFIRMED;
        this.updatedAt = now;
    }

    public void cancel(LocalDateTime now) {
        this.status = LeaveRequestStatus.CANCELLED;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LeaveRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
