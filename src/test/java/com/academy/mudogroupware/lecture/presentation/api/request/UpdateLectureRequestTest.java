package com.academy.mudogroupware.lecture.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.model.ClassType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UpdateLectureRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void convertsLegacySingleScheduleToUpdateCommand() {
        UpdateLectureRequest request = new UpdateLectureRequest(
                "Math advanced",
                ClassType.SPECIAL,
                DayOfWeek.TUESDAY,
                "B201",
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null,
                "Teacher Kim",
                "Math",
                "2026 Summer",
                null,
                null,
                null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(1L, 99L).lectureId()).isEqualTo(1L);
        assertThat(request.toCommand(1L, 99L).requesterId()).isEqualTo(99L);
        assertThat(request.toCommand(1L, 99L).schedules())
                .containsExactly(new ScheduleInput(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    void convertsMultipleSchedulesToUpdateCommand() {
        UpdateLectureRequest request = new UpdateLectureRequest(
                "Math advanced",
                ClassType.SPECIAL,
                null,
                "B201",
                null,
                null,
                null,
                "Teacher Kim",
                "Math",
                "2026 Summer",
                null,
                null,
                List.of(
                        new ScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        new ScheduleRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))));

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(1L, 99L).schedules())
                .containsExactly(
                        new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        new ScheduleInput(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }
}
