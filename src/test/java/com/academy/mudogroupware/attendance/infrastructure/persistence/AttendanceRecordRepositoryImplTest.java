package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

class AttendanceRecordRepositoryImplTest {

    @Test
    void preservesDuplicateCheckInConstraintViolationAsCause() {
        AttendanceRecordJpaRepository jpaRepository = mock(AttendanceRecordJpaRepository.class);
        AttendanceRecordRepositoryImpl repository = new AttendanceRecordRepositoryImpl(jpaRepository);
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Duplicate entry for key 'uk_attendance_record_academy_user_date'");
        when(jpaRepository.saveAndFlush(any(AttendanceRecordJpaEntity.class)))
                .thenThrow(violation);
        AttendanceRecord record = AttendanceRecord.checkIn(
                10L,
                LocalDateTime.of(2026, 8, 5, 9, 0),
                LocalTime.of(9, 0),
                0,
                null);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> repository.save(record));

        assertSame(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_IN,
                exception.getErrorCode());
        assertSame(violation, exception.getCause());
    }
}
