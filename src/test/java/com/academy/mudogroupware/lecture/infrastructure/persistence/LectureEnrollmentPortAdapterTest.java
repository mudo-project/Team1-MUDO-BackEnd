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

import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.model.Student;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;
import com.academy.mudogroupware.rollcall.application.port.EnrolledStudentRef;
import com.academy.mudogroupware.rollcall.application.port.LectureRef;

class LectureEnrollmentPortAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);

    private LectureEnrollmentPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LectureEnrollmentPortAdapter(lectureRepository, enrollmentRepository, studentRepository);
    }

    @Test
    void findLectureReturnsEmptyWhenNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(adapter.findLecture(1L)).isEmpty();
    }

    @Test
    void findLectureMapsToLectureRef() {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        Lecture lecture = Lecture.create(9L, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, FeeType.PER_SESSION,
                50000, List.of(schedule), NOW);
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

        Optional<LectureRef> ref = adapter.findLecture(1L);

        assertThat(ref).isPresent();
        assertThat(ref.get().name()).isEqualTo("수학 기초반");
        assertThat(ref.get().academyId()).isEqualTo(9L);
    }

    @Test
    void getEnrolledStudentsMapsStudentsFromEnrollments() {
        when(enrollmentRepository.findByLectureId(1L)).thenReturn(List.of(Enrollment.restore(1L, 5L, 1L, NOW)));
        when(studentRepository.findAllById(List.of(5L))).thenReturn(List.of(
                Student.restore(5L, 9L, "이준호", Grade.MIDDLE_3, null, null, "010-1111-1111", null, NOW)));

        List<EnrolledStudentRef> refs = adapter.getEnrolledStudents(1L);

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).name()).isEqualTo("이준호");
        assertThat(refs.get(0).grade()).isEqualTo("MIDDLE_3");
        assertThat(refs.get(0).parentPhone()).isEqualTo("010-1111-1111");
    }
}
