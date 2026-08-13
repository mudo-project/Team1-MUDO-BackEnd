package com.academy.mudogroupware.lecture.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.lecture.application.command.DeleteLectureCommand;
import com.academy.mudogroupware.lecture.domain.exception.LectureNotFoundException;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;

class DeleteLectureServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 10, 0);

    private final LectureRepository lectureRepository = org.mockito.Mockito.mock(LectureRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private DeleteLectureService service;

    @BeforeEach
    void setUp() {
        service = new DeleteLectureService(lectureRepository, clock);
    }

    @Test
    void deletesExistingLectureSoftly() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(existingLecture()));

        service.deleteLecture(new DeleteLectureCommand(1L, 99L));

        verify(lectureRepository).deleteById(1L, NOW);
    }

    @Test
    void throwsWhenLectureDoesNotExist() {
        when(lectureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLecture(new DeleteLectureCommand(1L, 99L)))
                .isInstanceOf(LectureNotFoundException.class);

        verify(lectureRepository, never()).deleteById(1L, NOW);
    }

    private Lecture existingLecture() {
        return Lecture.restore(
                1L,
                "Math",
                ClassType.CLASS,
                "A101",
                Grade.HIGH_1,
                null,
                null,
                null,
                3L,
                "Teacher A",
                "Math",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))),
                LocalDateTime.of(2026, 8, 1, 9, 0));
    }
}
