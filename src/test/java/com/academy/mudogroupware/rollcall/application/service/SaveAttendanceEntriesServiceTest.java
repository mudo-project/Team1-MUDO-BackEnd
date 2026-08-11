package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.command.AttendanceEntryInput;
import com.academy.mudogroupware.rollcall.application.command.SaveAttendanceEntriesCommand;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;
import com.academy.mudogroupware.rollcall.domain.exception.RollcallLectureNotFoundException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceEntryRepository;

class SaveAttendanceEntriesServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureEnrollmentPort lectureEnrollmentPort = mock(LectureEnrollmentPort.class);
    private final AttendanceEntryRepository attendanceEntryRepository = mock(AttendanceEntryRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private SaveAttendanceEntriesService service;

    @BeforeEach
    void setUp() {
        service = new SaveAttendanceEntriesService(lectureEnrollmentPort, attendanceEntryRepository, clock);
    }

    private SaveAttendanceEntriesCommand command(List<AttendanceEntryInput> entries) {
        return new SaveAttendanceEntriesCommand(LECTURE_ID, DATE, entries);
    }

    @Test
    void throwsWhenLectureNotFound() {
        when(lectureEnrollmentPort.findLecture(LECTURE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveEntries(command(List.of())))
                .isInstanceOf(RollcallLectureNotFoundException.class);
    }

    @Test
    void createsNewEntryWhenNoneExists() {
        when(lectureEnrollmentPort.findLecture(LECTURE_ID))
                .thenReturn(Optional.of(new LectureRef(LECTURE_ID, "수학 기초반")));
        when(attendanceEntryRepository.findByLectureIdAndDate(LECTURE_ID, DATE)).thenReturn(List.of());
        when(attendanceEntryRepository.findByLectureIdAndStudentIdAndDate(LECTURE_ID, 5L, DATE))
                .thenReturn(Optional.empty());

        service.saveEntries(command(List.of(new AttendanceEntryInput(5L, AttendanceStatus.PRESENT, null))));

        verify(attendanceEntryRepository).findByLectureIdAndDate(LECTURE_ID, DATE);
        verify(attendanceEntryRepository, never()).findByLectureIdAndStudentIdAndDate(eq(LECTURE_ID), any(), eq(DATE));
        verify(attendanceEntryRepository).save(any(AttendanceEntry.class));
    }

    @Test
    void updatesExistingEntryWhenPresent() {
        when(lectureEnrollmentPort.findLecture(LECTURE_ID))
                .thenReturn(Optional.of(new LectureRef(LECTURE_ID, "수학 기초반")));
        AttendanceEntry existing = AttendanceEntry.create(LECTURE_ID, 5L, DATE, AttendanceStatus.PRESENT,
                null, NOW.minusDays(1));
        when(attendanceEntryRepository.findByLectureIdAndDate(LECTURE_ID, DATE)).thenReturn(List.of(existing));
        when(attendanceEntryRepository.findByLectureIdAndStudentIdAndDate(LECTURE_ID, 5L, DATE))
                .thenReturn(Optional.of(existing));

        service.saveEntries(command(List.of(new AttendanceEntryInput(5L, AttendanceStatus.LATE, null))));

        verify(attendanceEntryRepository).findByLectureIdAndDate(LECTURE_ID, DATE);
        verify(attendanceEntryRepository, never()).findByLectureIdAndStudentIdAndDate(eq(LECTURE_ID), any(), eq(DATE));
        verify(attendanceEntryRepository, times(1)).save(existing);
    }
}
