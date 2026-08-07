package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

/**
 * approval 모듈에서 승인된 휴가 기간을 attendance 쪽에서 자체적으로 들고 있기 위한 모델.
 * approval의 ApprovalDocument를 직접 참조하지 않고 documentId(식별자)만 보관한다.
 * 신청 시점에는 동기 Port로 생성되고 최종 승인·반려 이벤트를 소비해 상태가 갱신된다.
 */
public final class LeaveRequest {

    private final Long id;
    private final Long academyId;
    private final Long userId;
    private final Long documentId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int usedDays;
    private LeaveRequestStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LeaveRequest(Long id, Long academyId, Long userId, Long documentId, LocalDate startDate,
                         LocalDate endDate, int usedDays, LeaveRequestStatus status, LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
        if (academyId == null || userId == null || documentId == null || startDate == null || endDate == null
                || endDate.isBefore(startDate) || usedDays <= 0 || status == null || createdAt == null
                || updatedAt == null) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_LEAVE_PERIOD);
        }
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.documentId = documentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usedDays = usedDays;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LeaveRequest submit(Long academyId, Long userId, Long documentId, LocalDate startDate,
                                      LocalDate endDate, int usedDays, LocalDateTime now) {
        return new LeaveRequest(null, academyId, userId, documentId, startDate, endDate, usedDays,
                LeaveRequestStatus.PENDING, now, now);
    }

    public static LeaveRequest restore(Long id, Long academyId, Long userId, Long documentId, LocalDate startDate,
                                       LocalDate endDate, int usedDays, LeaveRequestStatus status, LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        return new LeaveRequest(id, academyId, userId, documentId, startDate, endDate, usedDays, status, createdAt,
                updatedAt);
    }

    public void approve(LocalDateTime now) {
        if (status == LeaveRequestStatus.APPROVED) {
            return;
        }
        if (status != LeaveRequestStatus.PENDING) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_LEAVE_REQUEST_STATE);
        }
        this.status = LeaveRequestStatus.APPROVED;
        this.updatedAt = now;
    }

    public void reject(LocalDateTime now) {
        if (status == LeaveRequestStatus.REJECTED) {
            return;
        }
        if (status != LeaveRequestStatus.PENDING) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_LEAVE_REQUEST_STATE);
        }
        this.status = LeaveRequestStatus.REJECTED;
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

    public int getUsedDays() {
        return usedDays;
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
