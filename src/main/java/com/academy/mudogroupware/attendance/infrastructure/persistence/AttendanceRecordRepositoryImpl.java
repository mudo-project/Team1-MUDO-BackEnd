package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceRecordRepositoryImpl implements AttendanceRecordRepository {

    private static final String UNIQUE_CONSTRAINT_NAME =
            "uk_attendance_record_academy_user_date";

    private final AttendanceRecordJpaRepository attendanceRecordJpaRepository;

    @Override
    public boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate) {
        return attendanceRecordJpaRepository.existsByAcademyIdAndUserIdAndWorkDate(
                academyId, userId, workDate);
    }

    @Override
    public AttendanceRecord save(AttendanceRecord record) {
        AttendanceRecordJpaEntity entity = AttendanceRecordJpaEntity.builder()
                .id(record.getId())
                .academyId(record.getAcademyId())
                .userId(record.getUserId())
                .workDate(record.getWorkDate())
                .clockInAt(record.getClockInAt())
                .clockInNote(record.getClockInNote())
                .clockOutAt(record.getClockOutAt())
                .clockOutNote(record.getClockOutNote())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
        try {
            return toDomain(attendanceRecordJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateCheckIn(e)) {
                throw new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_IN, e);
            }
            throw e;
        }
    }

    private AttendanceRecord toDomain(AttendanceRecordJpaEntity entity) {
        return AttendanceRecord.restore(
                entity.getId(), entity.getAcademyId(), entity.getUserId(),
                entity.getWorkDate(), entity.getClockInAt(), entity.getClockInNote(),
                entity.getClockOutAt(), entity.getClockOutNote(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private boolean isDuplicateCheckIn(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(UNIQUE_CONSTRAINT_NAME)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
