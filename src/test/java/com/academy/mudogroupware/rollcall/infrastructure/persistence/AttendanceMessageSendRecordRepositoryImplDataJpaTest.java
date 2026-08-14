package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({AttendanceMessageSendRecordRepositoryImpl.class, TimeConfig.class})
class AttendanceMessageSendRecordRepositoryImplDataJpaTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    @Autowired
    private AttendanceMessageSendRecordRepositoryImpl attendanceMessageSendRecordRepository;

    @Test
    void createsANewPendingRecordWhenNoneExistsYet() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, NOW);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getStatus()).isEqualTo(AttendanceMessageSendStatus.PENDING);
    }

    @Test
    void returnsTheExistingRecordInsteadOfCreatingADuplicate() {
        AttendanceMessageSendRecord first = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, NOW);
        first.markResult(AttendanceMessageSendStatus.SENT, NOW);
        attendanceMessageSendRecordRepository.save(first);

        AttendanceMessageSendRecord second = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, NOW);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo(AttendanceMessageSendStatus.SENT);
    }

    @Test
    void savePersistsTheUpdatedStatus() {
        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, NOW);

        record.markResult(AttendanceMessageSendStatus.INDETERMINATE, NOW);
        attendanceMessageSendRecordRepository.save(record);

        AttendanceMessageSendRecord reloaded = attendanceMessageSendRecordRepository
                .createOrGetExisting(1L, 10L, DATE, NOW);
        assertThat(reloaded.getStatus()).isEqualTo(AttendanceMessageSendStatus.INDETERMINATE);
    }
}
