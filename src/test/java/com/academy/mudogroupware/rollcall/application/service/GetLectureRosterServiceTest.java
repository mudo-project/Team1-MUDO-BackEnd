package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureEnrollmentPort;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.domain.exception.RollcallLectureNotFoundException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceEntryRepository;

class GetLectureRosterServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureEnrollmentPort lectureEnrollmentPort = mock(LectureEnrollmentPort.class);
    private final AttendanceEntryRepository attendanceEntryRepository = mock(AttendanceEntryRepository.class);

    private GetLectureRosterService service;

    @BeforeEach
    void setUp() {
        service = new GetLectureRosterService(lectureEnrollmentPort, attendanceEntryRepository);
    }

    @Test
    void throwsWhenLectureNotFound() {
        when(lectureEnrollmentPort.findLecture(LECTURE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRoster(LECTURE_ID, DATE))
                .isInstanceOf(RollcallLectureNotFoundException.class);
    }

    @Test
    void mergesEnrolledStudentsWithAttendanceEntriesAndSummarizes() {
        when(lectureEnrollmentPort.findLecture(LECTURE_ID))
                .thenReturn(Optional.of(new LectureRef(LECTURE_ID, "수학 기초반")));
        when(lectureEnrollmentPort.getEnrolledStudents(LECTURE_ID)).thenReturn(List.of(
                new EnrolledStudentRef(10L, "이준호", "MIDDLE_3", "010-1111-1111"),
                new EnrolledStudentRef(20L, "김서윤", "HIGH_1", "010-2222-2222")));
        when(attendanceEntryRepository.findByLectureIdAndDate(LECTURE_ID, DATE)).thenReturn(List.of(
                AttendanceEntry.create(LECTURE_ID, 10L, DATE, AttendanceStatus.PRESENT, null, NOW)));

        RosterView roster = service.getRoster(LECTURE_ID, DATE);

        assertThat(roster.entries()).hasSize(2);
        assertThat(roster.entries().get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(roster.entries().get(1).status()).isNull();
        assertThat(roster.summary().total()).isEqualTo(2);
        assertThat(roster.summary().present()).isEqualTo(1);
        assertThat(roster.summary().absent()).isZero();
    }
}
