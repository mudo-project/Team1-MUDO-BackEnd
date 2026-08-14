package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.command.UpdateLectureCommand;
import com.academy.mudogroupware.lecture.domain.exception.ClassroomTimeConflictException;
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

class UpdateLectureServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 1, 9, 0);

    private final TermRepository termRepository = org.mockito.Mockito.mock(TermRepository.class);
    private final SubjectRepository subjectRepository = org.mockito.Mockito.mock(SubjectRepository.class);
    private final ClassroomRepository classroomRepository = org.mockito.Mockito.mock(ClassroomRepository.class);
    private final LectureRepository lectureRepository = org.mockito.Mockito.mock(LectureRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private UpdateLectureService service;

    @BeforeEach
    void setUp() {
        service = new UpdateLectureService(termRepository, subjectRepository, classroomRepository,
                lectureRepository, clock);
    }

    @Test
    void updatesLectureAndExcludesItselfFromOverlapCheck() {
        Lecture existing = existingLecture();
        UpdateLectureCommand command = command();

        when(lectureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(termRepository.findByName("2026 Summer")).thenReturn(Optional.of(Term.restore(10L, "2026 Summer", NOW)));
        when(subjectRepository.findByName("Math")).thenReturn(Optional.of(Subject.restore(20L, "Math", NOW)));
        when(classroomRepository.findByNameForUpdate("B201"))
                .thenReturn(Optional.of(Classroom.restore(30L, "B201", NOW)));
        when(lectureRepository.existsOverlapExcludingLecture(1L, "B201", DayOfWeek.TUESDAY,
                LocalTime.of(10, 0), LocalTime.of(12, 0))).thenReturn(false);
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateLecture(command);

        ArgumentCaptor<Lecture> lectureCaptor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(lectureCaptor.capture());

        Lecture updated = lectureCaptor.getValue();
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Updated Math");
        assertThat(updated.getClassType()).isEqualTo(ClassType.SPECIAL);
        assertThat(updated.getClassroomCode()).isEqualTo("B201");
        assertThat(updated.getGrade()).isEqualTo(Grade.HIGH_2);
        assertThat(updated.getTermId()).isEqualTo(10L);
        assertThat(updated.getSubjectId()).isEqualTo(20L);
        assertThat(updated.getTeacherName()).isEqualTo("Teacher B");
        assertThat(updated.getSubjectName()).isEqualTo("Math");
        assertThat(updated.getFeeType()).isEqualTo(FeeType.PER_MONTH);
        assertThat(updated.getFeeAmount()).isEqualTo(320000);
        assertThat(updated.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(updated.getSchedules())
                .extracting(LectureSchedule::getDayOfWeek, LectureSchedule::getStartTime, LectureSchedule::getEndTime)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(DayOfWeek.TUESDAY,
                        LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    void throwsWhenLectureDoesNotExist() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLecture(command()))
                .isInstanceOf(LectureNotFoundException.class);

        verify(lectureRepository, never()).save(any());
    }

    @Test
    void throwsWhenUpdatedScheduleConflictsWithAnotherLecture() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(existingLecture()));
        when(termRepository.findByName("2026 Summer")).thenReturn(Optional.of(Term.restore(10L, "2026 Summer", NOW)));
        when(subjectRepository.findByName("Math")).thenReturn(Optional.of(Subject.restore(20L, "Math", NOW)));
        when(classroomRepository.findByNameForUpdate("B201"))
                .thenReturn(Optional.of(Classroom.restore(30L, "B201", NOW)));
        when(lectureRepository.existsOverlapExcludingLecture(1L, "B201", DayOfWeek.TUESDAY,
                LocalTime.of(10, 0), LocalTime.of(12, 0))).thenReturn(true);

        assertThatThrownBy(() -> service.updateLecture(command()))
                .isInstanceOf(ClassroomTimeConflictException.class);

        verify(lectureRepository, never()).save(any());
    }

    private UpdateLectureCommand command() {
        return new UpdateLectureCommand(
                1L,
                "Updated Math",
                ClassType.SPECIAL,
                "B201",
                Grade.HIGH_2,
                "Teacher B",
                "Math",
                "2026 Summer",
                FeeType.PER_MONTH,
                320000,
                List.of(new ScheduleInput(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))),
                99L,
                null);
    }

    private Lecture existingLecture() {
        return Lecture.restore(
                1L,
                "Old Math",
                ClassType.CLASS,
                "A101",
                Grade.HIGH_1,
                1L,
                2L,
                null,
                3L,
                "Teacher A",
                "Old Subject",
                FeeType.PER_SESSION,
                50000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))),
                CREATED_AT);
    }
}
