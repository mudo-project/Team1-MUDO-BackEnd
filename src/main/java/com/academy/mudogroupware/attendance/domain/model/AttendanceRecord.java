package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

public final class AttendanceRecord {

    private final Long id;
    private final Long userId;
    private final LocalDate workDate;
    private final LocalDateTime clockInAt;
    private final String clockInNote;
    private final LocalDateTime clockOutAt;
    private final String clockOutNote;
    private final ClockOutType clockOutType;
    private final AttendanceStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AttendanceRecord(Long id, Long userId, LocalDate workDate,
                             LocalDateTime clockInAt, String clockInNote,
                             LocalDateTime clockOutAt, String clockOutNote,
                             ClockOutType clockOutType,
                             AttendanceStatus status, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.workDate = workDate;
        this.clockInAt = clockInAt;
        this.clockInNote = clockInNote;
        this.clockOutAt = clockOutAt;
        this.clockOutNote = clockOutNote;
        this.clockOutType = clockOutType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AttendanceRecord checkIn(Long userId,
                                           LocalDateTime clockInAt,
                                           LocalTime workStartTime,
                                           int lateGraceMinutes,
                                           String clockInNote) {
        if (userId == null || clockInAt == null
                || workStartTime == null || lateGraceMinutes < 0) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_RECORD);
        }
        String normalizedNote = normalizeNote(clockInNote);
        if (normalizedNote != null && normalizedNote.length() > 255) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_CLOCK_IN_NOTE);
        }
        LocalDate workDate = clockInAt.toLocalDate();
        LocalDateTime lateThreshold = workDate.atTime(workStartTime)
                .plusMinutes(lateGraceMinutes);
        AttendanceStatus status = clockInAt.isAfter(lateThreshold)
                ? AttendanceStatus.LATE
                : AttendanceStatus.NORMAL;
        if (status == AttendanceStatus.LATE && normalizedNote == null) {
            throw new AttendanceException(AttendanceErrorCode.LATE_NOTE_REQUIRED);
        }
        return new AttendanceRecord(null, userId, workDate,
                clockInAt, normalizedNote, null, null, null,
                status, clockInAt, clockInAt);
    }

    public static AttendanceRecord restore(Long id, Long userId,
                                           LocalDate workDate, LocalDateTime clockInAt,
                                           String clockInNote, LocalDateTime clockOutAt,
                                           String clockOutNote, ClockOutType clockOutType,
                                           AttendanceStatus status,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AttendanceRecord(id, userId, workDate, clockInAt,
                clockInNote, clockOutAt, clockOutNote, clockOutType,
                status, createdAt, updatedAt);
    }

    public AttendanceRecord checkOut(
            LocalDateTime checkedOutAt, ClockOutType type, String note) {
        if (checkedOutAt == null || clockInAt == null || checkedOutAt.isBefore(clockInAt)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_CLOCK_OUT_TIME);
        }
        if (type == null) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_RECORD);
        }
        if (clockOutAt != null) {
            throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT);
        }
        String normalizedNote = normalizeNote(note);
        if (normalizedNote != null && normalizedNote.length() > 255) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_CLOCK_OUT_NOTE);
        }
        if (type == ClockOutType.OVERTIME && normalizedNote == null) {
            throw new AttendanceException(AttendanceErrorCode.OVERTIME_NOTE_REQUIRED);
        }
        return new AttendanceRecord(id, userId, workDate,
                clockInAt, clockInNote, checkedOutAt, normalizedNote,
                type, status, createdAt, checkedOutAt);
    }

    public static AttendanceRecord createFromCorrection(Long userId,
                                                         LocalDate workDate,
                                                         LocalDateTime clockInAt,
                                                         String clockInNote,
                                                         LocalDateTime clockOutAt,
                                                         String clockOutNote,
                                                         AttendanceStatus status,
                                                         LocalDateTime now) {
        return new AttendanceRecord(null, userId, workDate, clockInAt, clockInNote,
                clockOutAt, clockOutNote,
                clockOutAt == null ? null : ClockOutType.NORMAL, status, now, now);
    }

    public AttendanceRecord applyCorrection(LocalDateTime correctedClockInAt,
                                             String correctedClockInNote,
                                             LocalDateTime correctedClockOutAt,
                                             String correctedClockOutNote,
                                             AttendanceStatus correctedStatus,
                                             LocalDateTime now) {
        if (correctedClockInAt == null
                || correctedClockOutAt != null && correctedClockOutAt.isBefore(correctedClockInAt)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_RECORD);
        }
        return new AttendanceRecord(id, userId, workDate, correctedClockInAt,
                correctedClockInNote, correctedClockOutAt, correctedClockOutNote,
                correctedClockOutAt == null ? null
                        : (clockOutType == null ? ClockOutType.NORMAL : clockOutType),
                correctedStatus, createdAt, now);
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalDateTime getClockInAt() { return clockInAt; }
    public String getClockInNote() { return clockInNote; }
    public LocalDateTime getClockOutAt() { return clockOutAt; }
    public String getClockOutNote() { return clockOutNote; }
    public ClockOutType getClockOutType() { return clockOutType; }
    public AttendanceStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
