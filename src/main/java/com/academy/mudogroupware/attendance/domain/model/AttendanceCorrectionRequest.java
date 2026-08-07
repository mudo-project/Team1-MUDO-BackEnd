package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

public final class AttendanceCorrectionRequest {

    private final Long id;
    private final Long academyId;
    private final Long userId;
    private final Long attendanceId;
    private final LocalDate workDate;
    private final AttendanceCorrectionType type;
    private final AttendanceCorrectionStatus status;
    private final LocalDateTime originalClockInAt;
    private final LocalDateTime originalClockOutAt;
    private final String originalClockInNote;
    private final String originalClockOutNote;
    private final LocalDateTime requestedClockInAt;
    private final LocalDateTime requestedClockOutAt;
    private final String requestedClockInNote;
    private final String requestedClockOutNote;
    private final String reason;
    private final LocalDateTime requestedAt;
    private final LocalDateTime processedAt;
    private final Long processedBy;
    private final String rejectionReason;

    private AttendanceCorrectionRequest(
            Long id, Long academyId, Long userId, Long attendanceId, LocalDate workDate,
            AttendanceCorrectionType type, AttendanceCorrectionStatus status,
            LocalDateTime originalClockInAt, LocalDateTime originalClockOutAt,
            String originalClockInNote, String originalClockOutNote,
            LocalDateTime requestedClockInAt, LocalDateTime requestedClockOutAt,
            String requestedClockInNote, String requestedClockOutNote,
            String reason, LocalDateTime requestedAt, LocalDateTime processedAt,
            Long processedBy, String rejectionReason) {
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.attendanceId = attendanceId;
        this.workDate = workDate;
        this.type = type;
        this.status = status;
        this.originalClockInAt = originalClockInAt;
        this.originalClockOutAt = originalClockOutAt;
        this.originalClockInNote = originalClockInNote;
        this.originalClockOutNote = originalClockOutNote;
        this.requestedClockInAt = requestedClockInAt;
        this.requestedClockOutAt = requestedClockOutAt;
        this.requestedClockInNote = requestedClockInNote;
        this.requestedClockOutNote = requestedClockOutNote;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.processedBy = processedBy;
        this.rejectionReason = rejectionReason;
    }

    public static AttendanceCorrectionRequest submit(
            Long academyId, Long userId, AttendanceRecord attendance, LocalDate workDate,
            AttendanceCorrectionType type, LocalDateTime requestedClockInAt,
            LocalDateTime requestedClockOutAt, String requestedClockInNote,
            String requestedClockOutNote, String reason, LocalDateTime requestedAt) {
        validateBase(academyId, userId, workDate, type, reason, requestedAt);
        validateRequestFields(
                type, attendance, workDate, requestedClockInAt, requestedClockOutAt,
                requestedClockInNote, requestedClockOutNote);
        String normalizedReason = normalize(reason);
        String normalizedClockInNote = normalize(requestedClockInNote);
        String normalizedClockOutNote = normalize(requestedClockOutNote);
        validateLengths(normalizedReason, normalizedClockInNote, normalizedClockOutNote);

        return new AttendanceCorrectionRequest(
                null, academyId, userId, attendance == null ? null : attendance.getId(), workDate,
                type, AttendanceCorrectionStatus.PENDING,
                attendance == null ? null : attendance.getClockInAt(),
                attendance == null ? null : attendance.getClockOutAt(),
                attendance == null ? null : attendance.getClockInNote(),
                attendance == null ? null : attendance.getClockOutNote(),
                requestedClockInAt, requestedClockOutAt,
                normalizedClockInNote, normalizedClockOutNote,
                normalizedReason, requestedAt, null, null, null);
    }

    public static AttendanceCorrectionRequest restore(
            Long id, Long academyId, Long userId, Long attendanceId, LocalDate workDate,
            AttendanceCorrectionType type, AttendanceCorrectionStatus status,
            LocalDateTime originalClockInAt, LocalDateTime originalClockOutAt,
            String originalClockInNote, String originalClockOutNote,
            LocalDateTime requestedClockInAt, LocalDateTime requestedClockOutAt,
            String requestedClockInNote, String requestedClockOutNote,
            String reason, LocalDateTime requestedAt, LocalDateTime processedAt,
            Long processedBy, String rejectionReason) {
        return new AttendanceCorrectionRequest(
                id, academyId, userId, attendanceId, workDate, type, status,
                originalClockInAt, originalClockOutAt, originalClockInNote, originalClockOutNote,
                requestedClockInAt, requestedClockOutAt, requestedClockInNote,
                requestedClockOutNote, reason, requestedAt, processedAt,
                processedBy, rejectionReason);
    }

    private static void validateBase(
            Long academyId, Long userId, LocalDate workDate, AttendanceCorrectionType type,
            String reason, LocalDateTime requestedAt) {
        if (academyId == null || userId == null || workDate == null || type == null
                || requestedAt == null || normalize(reason) == null) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_CORRECTION_REQUEST);
        }
    }

    private static void validateRequestFields(
            AttendanceCorrectionType type, AttendanceRecord attendance, LocalDate workDate,
            LocalDateTime requestedClockInAt, LocalDateTime requestedClockOutAt,
            String requestedClockInNote, String requestedClockOutNote) {
        boolean hasClockInNote = normalize(requestedClockInNote) != null;
        boolean hasClockOutNote = normalize(requestedClockOutNote) != null;
        switch (type) {
            case CLOCK_IN_TIME -> {
                requireAttendance(attendance);
                if (requestedClockInAt == null || requestedClockOutAt != null
                        || hasClockInNote || hasClockOutNote
                        || !requestedClockInAt.toLocalDate().equals(workDate)
                        || attendance.getClockOutAt() != null
                        && requestedClockInAt.isAfter(attendance.getClockOutAt())) {
                    invalid();
                }
            }
            case CLOCK_OUT_TIME -> {
                requireAttendance(attendance);
                if (requestedClockOutAt == null || requestedClockInAt != null
                        || hasClockInNote || hasClockOutNote
                        || !requestedClockOutAt.toLocalDate().equals(workDate)
                        || attendance.getClockInAt() == null
                        || requestedClockOutAt.isBefore(attendance.getClockInAt())) {
                    invalid();
                }
            }
            case MISSING_RECORD -> {
                if (attendance != null || requestedClockInAt == null || requestedClockOutAt == null
                        || !requestedClockInAt.toLocalDate().equals(workDate)
                        || !requestedClockOutAt.toLocalDate().equals(workDate)
                        || requestedClockOutAt.isBefore(requestedClockInAt)) {
                    invalid();
                }
            }
            case CLOCK_IN_NOTE -> {
                requireAttendance(attendance);
                if (!hasClockInNote || hasClockOutNote
                        || requestedClockInAt != null || requestedClockOutAt != null) {
                    invalid();
                }
            }
            case CLOCK_OUT_NOTE -> {
                requireAttendance(attendance);
                if (!hasClockOutNote || hasClockInNote
                        || requestedClockInAt != null || requestedClockOutAt != null) {
                    invalid();
                }
            }
        }
    }

    private static void requireAttendance(AttendanceRecord attendance) {
        if (attendance == null) {
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_ATTENDANCE_NOT_FOUND);
        }
    }

    private static void validateLengths(
            String reason, String requestedClockInNote, String requestedClockOutNote) {
        if (reason.length() > 500
                || requestedClockInNote != null && requestedClockInNote.length() > 255
                || requestedClockOutNote != null && requestedClockOutNote.length() > 255) {
            invalid();
        }
    }

    private static void invalid() {
        throw new AttendanceException(AttendanceErrorCode.INVALID_CORRECTION_REQUEST);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() { return id; }
    public Long getAcademyId() { return academyId; }
    public Long getUserId() { return userId; }
    public Long getAttendanceId() { return attendanceId; }
    public LocalDate getWorkDate() { return workDate; }
    public AttendanceCorrectionType getType() { return type; }
    public AttendanceCorrectionStatus getStatus() { return status; }
    public LocalDateTime getOriginalClockInAt() { return originalClockInAt; }
    public LocalDateTime getOriginalClockOutAt() { return originalClockOutAt; }
    public String getOriginalClockInNote() { return originalClockInNote; }
    public String getOriginalClockOutNote() { return originalClockOutNote; }
    public LocalDateTime getRequestedClockInAt() { return requestedClockInAt; }
    public LocalDateTime getRequestedClockOutAt() { return requestedClockOutAt; }
    public String getRequestedClockInNote() { return requestedClockInNote; }
    public String getRequestedClockOutNote() { return requestedClockOutNote; }
    public String getReason() { return reason; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public Long getProcessedBy() { return processedBy; }
    public String getRejectionReason() { return rejectionReason; }

    public AttendanceCorrectionRequest approve(Long processorId, LocalDateTime processedAt) {
        validatePending();
        return new AttendanceCorrectionRequest(id, academyId, userId, attendanceId, workDate, type,
                AttendanceCorrectionStatus.APPROVED, originalClockInAt, originalClockOutAt,
                originalClockInNote, originalClockOutNote, requestedClockInAt, requestedClockOutAt,
                requestedClockInNote, requestedClockOutNote, reason, requestedAt, processedAt,
                processorId, null);
    }

    public AttendanceCorrectionRequest attachAttendanceId(Long nextAttendanceId) {
        return new AttendanceCorrectionRequest(id, academyId, userId, nextAttendanceId, workDate,
                type, status, originalClockInAt, originalClockOutAt, originalClockInNote,
                originalClockOutNote, requestedClockInAt, requestedClockOutAt, requestedClockInNote,
                requestedClockOutNote, reason, requestedAt, processedAt, processedBy, rejectionReason);
    }

    public AttendanceCorrectionRequest reject(Long processorId, LocalDateTime processedAt,
                                               String rejectionReason) {
        validatePending();
        String normalized = normalize(rejectionReason);
        if (normalized == null || normalized.length() > 500) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_CORRECTION_REJECTION_REASON);
        }
        return new AttendanceCorrectionRequest(id, academyId, userId, attendanceId, workDate, type,
                AttendanceCorrectionStatus.REJECTED, originalClockInAt, originalClockOutAt,
                originalClockInNote, originalClockOutNote, requestedClockInAt, requestedClockOutAt,
                requestedClockInNote, requestedClockOutNote, reason, requestedAt, processedAt,
                processorId, normalized);
    }

    private void validatePending() {
        if (status != AttendanceCorrectionStatus.PENDING) {
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_ALREADY_PROCESSED);
        }
    }
}
