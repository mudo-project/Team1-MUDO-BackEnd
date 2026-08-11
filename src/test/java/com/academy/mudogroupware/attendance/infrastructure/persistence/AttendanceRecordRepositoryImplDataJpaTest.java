package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(AttendanceRecordRepositoryImpl.class)
class AttendanceRecordRepositoryImplDataJpaTest {

    @Autowired
    private AttendanceRecordRepositoryImpl attendanceRecordRepository;

    @Test
    void returnsOnlyRecordsForRequestedUserIdsAndWorkDate() {
        LocalDate today = LocalDate.of(2026, 8, 5);
        LocalDate yesterday = today.minusDays(1);
        attendanceRecordRepository.save(AttendanceRecord.checkIn(
                1L, today.atTime(8, 52), LocalTime.of(9, 0), 10, null));
        attendanceRecordRepository.save(AttendanceRecord.checkIn(
                2L, today.atTime(8, 40), LocalTime.of(9, 0), 10, null));
        attendanceRecordRepository.save(AttendanceRecord.checkIn(
                3L, yesterday.atTime(8, 40), LocalTime.of(9, 0), 10, null));

        List<AttendanceRecord> result = attendanceRecordRepository
                .findAllByUserIdsAndWorkDate(List.of(1L, 2L), today);

        assertThat(result).extracting(AttendanceRecord::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void returnsEmptyListWhenNoMatchingRecords() {
        List<AttendanceRecord> result = attendanceRecordRepository
                .findAllByUserIdsAndWorkDate(List.of(99L), LocalDate.of(2026, 8, 5));

        assertThat(result).isEmpty();
    }
}
