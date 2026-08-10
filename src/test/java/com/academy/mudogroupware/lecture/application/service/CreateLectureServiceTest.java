package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.lecture.domain.repository.SubjectRepository;
import com.academy.mudogroupware.lecture.domain.repository.TermRepository;

class CreateLectureServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final TermRepository termRepository = mock(TermRepository.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private CreateLectureService service;

    @BeforeEach
    void setUp() {
        service = new CreateLectureService(termRepository, subjectRepository, classroomRepository,
                lectureRepository, clock);
    }

    private CreateLectureCommand command() {
        return new CreateLectureCommand("수학 기초반", Grade.MIDDLE_3, "2026 겨울방학 특강", "수학", 30L, "101호",
                FeeType.PER_SESSION, 50000, List.of(new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(15, 0),
                LocalTime.of(17, 0))), 99L);
    }

    @Test
    void reusesExistingTermSubjectClassroomWhenFound() {
        when(termRepository.findByName("2026 겨울방학 특강"))
                .thenReturn(Optional.of(Term.restore(10L, "2026 겨울방학 특강", NOW)));
        when(subjectRepository.findByName("수학"))
                .thenReturn(Optional.of(Subject.restore(20L, "수학", NOW)));
        when(classroomRepository.findByName("101호"))
                .thenReturn(Optional.of(Classroom.restore(40L, "101호", NOW)));
        when(lectureRepository.existsOverlap(40L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0)))
                .thenReturn(false);
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLecture(command());

        verify(termRepository, never()).save(any());
        verify(subjectRepository, never()).save(any());
        verify(classroomRepository, never()).save(any());
    }

    @Test
    void createsNewTermSubjectClassroomWhenNotFound() {
        when(termRepository.findByName("2026 겨울방학 특강")).thenReturn(Optional.empty());
        when(termRepository.save(any(Term.class))).thenReturn(Term.restore(10L, "2026 겨울방학 특강", NOW));
        when(subjectRepository.findByName("수학")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenReturn(Subject.restore(20L, "수학", NOW));
        when(classroomRepository.findByName("101호")).thenReturn(Optional.empty());
        when(classroomRepository.save(any(Classroom.class)))
                .thenReturn(Classroom.restore(40L, "101호", NOW));
        when(lectureRepository.existsOverlap(40L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0)))
                .thenReturn(false);
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLecture(command());

        verify(termRepository).save(any(Term.class));
        verify(subjectRepository).save(any(Subject.class));
        verify(classroomRepository).save(any(Classroom.class));
    }

    @Test
    void throwsWhenClassroomTimeConflictExists() {
        when(termRepository.findByName("2026 겨울방학 특강"))
                .thenReturn(Optional.of(Term.restore(10L, "2026 겨울방학 특강", NOW)));
        when(subjectRepository.findByName("수학"))
                .thenReturn(Optional.of(Subject.restore(20L, "수학", NOW)));
        when(classroomRepository.findByName("101호"))
                .thenReturn(Optional.of(Classroom.restore(40L, "101호", NOW)));
        when(lectureRepository.existsOverlap(40L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createLecture(command()))
                .isInstanceOf(ClassroomTimeConflictException.class);

        verify(lectureRepository, never()).save(any());
    }

    @Test
    void createdLectureCarriesCommandValues() {
        when(termRepository.findByName("2026 겨울방학 특강"))
                .thenReturn(Optional.of(Term.restore(10L, "2026 겨울방학 특강", NOW)));
        when(subjectRepository.findByName("수학"))
                .thenReturn(Optional.of(Subject.restore(20L, "수학", NOW)));
        when(classroomRepository.findByName("101호"))
                .thenReturn(Optional.of(Classroom.restore(40L, "101호", NOW)));
        when(lectureRepository.existsOverlap(40L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0)))
                .thenReturn(false);
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> {
            Lecture saved = invocation.getArgument(0);
            return Lecture.restore(1L, saved.getName(), saved.getGrade(), saved.getTermId(),
                    saved.getSubjectId(), saved.getTeacherId(), saved.getClassroomId(), saved.getFeeType(),
                    saved.getFeeAmount(), saved.getSchedules(), saved.getCreatedAt());
        });

        Long lectureId = service.createLecture(command());

        assertThat(lectureId).isEqualTo(1L);
    }
}
