package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

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
                .clockOutType(record.getClockOutType())
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

    @Override
    public Optional<AttendanceRecord> findLatestOpenSince(
            Long academyId, Long userId, LocalDate earliestWorkDate) {
        return attendanceRecordJpaRepository
                .findFirstByAcademyIdAndUserIdAndWorkDateGreaterThanEqualAndClockOutAtIsNullOrderByClockInAtDesc(
                        academyId, userId, earliestWorkDate)
                .map(this::toDomain);
    }

    @Override
    public boolean existsCheckedOutBetween(
            Long academyId, Long userId, LocalDateTime from, LocalDateTime to) {
        return attendanceRecordJpaRepository
                .existsByAcademyIdAndUserIdAndClockOutAtBetween(
                        academyId, userId, from, to);
    }

    @Override
    public Optional<AttendanceRecord> findByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate) {
        return attendanceRecordJpaRepository
                .findByAcademyIdAndUserIdAndWorkDate(academyId, userId, workDate)
                .map(this::toDomain);
    }

    @Override
    public List<AttendanceRecord> findByAcademyIdAndUserIdAndWorkDateBetween(
            Long academyId, Long userId, LocalDate startDate, LocalDate endDate) {
        return attendanceRecordJpaRepository
                .findAllByAcademyIdAndUserIdAndWorkDateBetweenOrderByWorkDate(
                        academyId, userId, startDate, endDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AttendanceRecord toDomain(AttendanceRecordJpaEntity entity) {
        return AttendanceRecord.restore(
                entity.getId(), entity.getAcademyId(), entity.getUserId(),
                entity.getWorkDate(), entity.getClockInAt(), entity.getClockInNote(),
                entity.getClockOutAt(), entity.getClockOutNote(),
                entity.getClockOutType(), entity.getStatus(),
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
