package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.lecture.application.port.EnrolledStudentsPort;
import com.academy.mudogroupware.lecture.application.port.TeacherDirectoryPort;
import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.application.query.LectureSummaryView;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
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
    private final TeacherDirectoryPort teacherDirectoryPort = mock(TeacherDirectoryPort.class);

    private LectureQueryService service;

    @BeforeEach
    void setUp() {
        service = new LectureQueryService(lectureRepository, termRepository, subjectRepository, classroomRepository,
                enrolledStudentsPort, teacherDirectoryPort);
    }

    private Lecture lecture(Long lectureId) {
        return lecture(lectureId, 30L);
    }

    private Lecture lecture(Long lectureId, Long teacherId) {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        return Lecture.restore(lectureId, "Math Basics", Grade.MIDDLE_3, 10L, 20L, teacherId, 40L,
                FeeType.PER_SESSION, 50000, List.of(schedule), NOW);
    }

    @Test
    void throwsWhenLectureNotFound() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLectureDetail(1L)).isInstanceOf(LectureNotFoundException.class);
    }

    @Test
    void returnsDetailWithResolvedNamesWhenAccessible() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture(1L)));
        when(termRepository.findAllById(List.of(10L))).thenReturn(List.of(Term.restore(10L, "Winter", NOW)));
        when(subjectRepository.findAllById(List.of(20L))).thenReturn(List.of(Subject.restore(20L, "Math", NOW)));
        when(classroomRepository.findAllById(List.of(40L)))
                .thenReturn(List.of(Classroom.restore(40L, "Room 101", NOW)));
        when(teacherDirectoryPort.findTeachers(List.of(30L)))
                .thenReturn(Map.of(30L, new TeacherInfo(30L, "Teacher Kim", 1L, "ACTIVE")));
        when(enrolledStudentsPort.findByLectureId(1L)).thenReturn(List.of());

        LectureDetailView view = service.getLectureDetail(1L);

        assertThat(view.termName()).isEqualTo("Winter");
        assertThat(view.subjectName()).isEqualTo("Math");
        assertThat(view.teacherName()).isEqualTo("Teacher Kim");
        assertThat(view.classroomName()).isEqualTo("Room 101");
    }

    @Test
    void returnsStoredTimetableFieldsBeforeLegacyLookupValues() {
        LectureSchedule schedule = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0),
                LocalTime.of(21, 0));
        Lecture lecture = Lecture.restore(1L, "Math Basics", ClassType.CLASS, "601", Grade.HIGH_1,
                null, null, null, null, "Stored Teacher", "Stored Subject", FeeType.PER_MONTH, 300000,
                List.of(schedule), NOW);
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));
        when(enrolledStudentsPort.findByLectureId(1L)).thenReturn(List.of());

        LectureDetailView view = service.getLectureDetail(1L);

        assertThat(view.classType()).isEqualTo(ClassType.CLASS);
        assertThat(view.classroomCode()).isEqualTo("601");
        assertThat(view.classroomName()).isEqualTo("601");
        assertThat(view.teacherName()).isEqualTo("Stored Teacher");
        assertThat(view.subjectName()).isEqualTo("Stored Subject");
    }

    @Test
    void returnsSummariesWithResolvedTeacherNames() {
        Lecture lecture = lecture(1L);
        when(lectureRepository.findAll(null, 0, 20))
                .thenReturn(PageResult.of(List.of(lecture), 0, 20, false));
        when(termRepository.findAllById(List.of(10L))).thenReturn(List.of(Term.restore(10L, "Winter", NOW)));
        when(subjectRepository.findAllById(List.of(20L))).thenReturn(List.of(Subject.restore(20L, "Math", NOW)));
        when(classroomRepository.findAllById(List.of(40L)))
                .thenReturn(List.of(Classroom.restore(40L, "Room 101", NOW)));
        when(teacherDirectoryPort.findTeachers(List.of(30L)))
                .thenReturn(Map.of(30L, new TeacherInfo(30L, "Teacher Kim", 1L, "ACTIVE")));
        when(enrolledStudentsPort.countByLectureIds(List.of(lecture.getId())))
                .thenReturn(Map.of(lecture.getId(), 0L));

        PageResult<LectureSummaryView> result = service.getLectures(null, 0, 20);

        assertThat(result.content()).extracting(LectureSummaryView::teacherName).containsExactly("Teacher Kim");
    }

    @Test
    void returnsSummariesWithStudentCountsInOneBatch() {
        Lecture first = lecture(1L, 30L);
        Lecture second = lecture(2L, 31L);
        when(lectureRepository.findAll(null, 0, 20))
                .thenReturn(PageResult.of(List.of(first, second), 0, 20, false));
        when(termRepository.findAllById(List.of(10L))).thenReturn(List.of(Term.restore(10L, "Winter", NOW)));
        when(subjectRepository.findAllById(List.of(20L))).thenReturn(List.of(Subject.restore(20L, "Math", NOW)));
        when(classroomRepository.findAllById(List.of(40L)))
                .thenReturn(List.of(Classroom.restore(40L, "Room 101", NOW)));
        when(teacherDirectoryPort.findTeachers(List.of(30L, 31L)))
                .thenReturn(Map.of(
                        30L, new TeacherInfo(30L, "Teacher Kim", 1L, "ACTIVE"),
                        31L, new TeacherInfo(31L, "Teacher Lee", 1L, "ACTIVE")));
        when(enrolledStudentsPort.countByLectureIds(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, 3L, 2L, 1L));

        PageResult<LectureSummaryView> result = service.getLectures(null, 0, 20);

        assertThat(result.content()).extracting(LectureSummaryView::studentCount)
                .containsExactly(3, 1);
        verify(enrolledStudentsPort).countByLectureIds(List.of(1L, 2L));
        verify(enrolledStudentsPort, never()).findByLectureId(1L);
    }

    @Test
    void returnsDistinctTeacherNamesFromRepository() {
        when(lectureRepository.findDistinctTeacherNames()).thenReturn(List.of("김선생", "이선생"));

        List<String> result = service.getTeacherNames();

        assertThat(result).containsExactly("김선생", "이선생");
    }
}
