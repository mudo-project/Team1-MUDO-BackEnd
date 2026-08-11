package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.port.EnrolledStudentInfo;
import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;

class LectureEnrollmentPortAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final EnrolledStudentsPort enrolledStudentsPort = mock(EnrolledStudentsPort.class);

    private LectureEnrollmentPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LectureEnrollmentPortAdapter(lectureRepository, enrolledStudentsPort);
    }

    private Lecture lecture() {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        return Lecture.create("수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, FeeType.PER_SESSION, 50000,
                List.of(schedule), NOW);
    }

    @Test
    void findLectureReturnsEmptyWhenNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(adapter.findLecture(1L)).isEmpty();
    }

    @Test
    void findLectureMapsToLectureRef() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture()));

        Optional<LectureRef> ref = adapter.findLecture(1L);

        assertThat(ref).isPresent();
        assertThat(ref.get().name()).isEqualTo("수학 기초반");
    }

    @Test
    void getEnrolledStudentsReturnsEmptyWhenLectureNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(adapter.getEnrolledStudents(1L)).isEmpty();
    }

    @Test
    void getEnrolledStudentsMapsFromEnrolledStudentsPort() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture()));
        when(enrolledStudentsPort.findByLectureId(1L)).thenReturn(List.of(
                new EnrolledStudentInfo(5L, "이준호", "MIDDLE_3", "010-1111-1111")));

        List<EnrolledStudentRef> refs = adapter.getEnrolledStudents(1L);

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).name()).isEqualTo("이준호");
        assertThat(refs.get(0).grade()).isEqualTo("MIDDLE_3");
        assertThat(refs.get(0).parentPhone()).isEqualTo("010-1111-1111");
    }
}
