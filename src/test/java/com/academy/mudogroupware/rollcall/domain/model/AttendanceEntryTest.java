package com.academy.mudogroupware.rollcall.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.domain.exception.EtcNoteRequiredException;

class AttendanceEntryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Test
    void createsEntryWithPresentStatus() {
        AttendanceEntry entry = AttendanceEntry.create(1L, 10L, 20L, DATE, AttendanceStatus.PRESENT, null, NOW);

        assertThat(entry.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(entry.getNote()).isNull();
    }

    @Test
    void throwsWhenEtcStatusHasNoNote() {
        assertThatThrownBy(() -> AttendanceEntry.create(1L, 10L, 20L, DATE, AttendanceStatus.ETC, null, NOW))
                .isInstanceOf(EtcNoteRequiredException.class);
    }

    @Test
    void discardsNoteWhenStatusIsNotEtc() {
        AttendanceEntry entry = AttendanceEntry.create(1L, 10L, 20L, DATE, AttendanceStatus.PRESENT, "무시될 사유", NOW);

        assertThat(entry.getNote()).isNull();
    }

    @Test
    void changeStatusUpdatesStatusAndUpdatedAt() {
        AttendanceEntry entry = AttendanceEntry.create(1L, 10L, 20L, DATE, AttendanceStatus.PRESENT, null, NOW);
        LocalDateTime later = NOW.plusHours(1);

        entry.changeStatus(AttendanceStatus.ETC, "조퇴", later);

        assertThat(entry.getStatus()).isEqualTo(AttendanceStatus.ETC);
        assertThat(entry.getNote()).isEqualTo("조퇴");
        assertThat(entry.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void changeStatusThrowsWhenEtcStatusHasNoNote() {
        AttendanceEntry entry = AttendanceEntry.create(1L, 10L, 20L, DATE, AttendanceStatus.PRESENT, null, NOW);

        assertThatThrownBy(() -> entry.changeStatus(AttendanceStatus.ETC, " ", NOW.plusHours(1)))
                .isInstanceOf(EtcNoteRequiredException.class);
    }
}
