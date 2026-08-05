package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.domain.exception.LectureAccessDeniedException;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.SubjectRepository;
import com.academy.mudogroupware.lecture.domain.repository.TermRepository;

class LectureQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final TermRepository termRepository = mock(TermRepository.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
    private final EnrolledStudentsPort enrolledStudentsPort = mock(EnrolledStudentsPort.class);

    private LectureQueryService service;

    @BeforeEach
    void setUp() {
        service = new LectureQueryService(lectureRepository, termRepository, subjectRepository, classroomRepository,
                enrolledStudentsPort);
    }

    private Lecture lecture(Long academyId) {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        return Lecture.create(academyId, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, FeeType.PER_SESSION, 50000,
                List.of(schedule), NOW);
    }

    @Test
    void throwsWhenLectureNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLectureDetail(1L, 1L)).isInstanceOf(LectureNotFoundException.class);
    }

    @Test
    void throwsWhenLectureBelongsToDifferentAcademy() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture(1L)));

        assertThatThrownBy(() -> service.getLectureDetail(1L, 2L))
                .isInstanceOf(LectureAccessDeniedException.class);
    }

    @Test
    void returnsDetailWithResolvedNamesWhenAccessible() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture(1L)));
        when(termRepository.findAllById(List.of(10L))).thenReturn(List.of(Term.restore(10L, 1L, "2026 겨울방학 특강", NOW)));
        when(subjectRepository.findAllById(List.of(20L)))
                .thenReturn(List.of(Subject.restore(20L, 1L, "수학", NOW)));
        when(classroomRepository.findAllById(List.of(40L)))
                .thenReturn(List.of(Classroom.restore(40L, 1L, "101호", NOW)));
        when(enrolledStudentsPort.findByLectureId(anyLong(), anyLong())).thenReturn(List.of());

        LectureDetailView view = service.getLectureDetail(1L, 1L);

        assertThat(view.termName()).isEqualTo("2026 겨울방학 특강");
        assertThat(view.subjectName()).isEqualTo("수학");
        assertThat(view.classroomName()).isEqualTo("101호");
    }
}
