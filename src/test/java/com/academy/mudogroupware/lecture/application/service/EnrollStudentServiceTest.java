package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.exception.StudentNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.model.Student;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

class EnrollStudentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private EnrollStudentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollStudentService(lectureRepository, studentRepository, enrollmentRepository, clock);
    }

    private Lecture lecture() {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        return Lecture.create(1L, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, FeeType.PER_SESSION, 50000,
                List.of(schedule), NOW);
    }

    @Test
    void throwsWhenLectureNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollStudent(new EnrollStudentCommand(1L, 2L)))
                .isInstanceOf(LectureNotFoundException.class);
    }

    @Test
    void throwsWhenStudentNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture()));
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollStudent(new EnrollStudentCommand(1L, 2L)))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void savesEnrollmentWhenLectureAndStudentExist() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture()));
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(Student.create(1L, "이준호", Grade.MIDDLE_3, null, null, null, null, NOW)));

        service.enrollStudent(new EnrollStudentCommand(1L, 2L));

        verify(enrollmentRepository).save(any(Enrollment.class));
    }
}
