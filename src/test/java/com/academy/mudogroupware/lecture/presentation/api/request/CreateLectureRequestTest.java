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

class CreateLectureRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsLegacyTimetableSlotShapeWithOnlyRequiredFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "Math regular",
                ClassType.CLASS,
                DayOfWeek.MONDAY,
                "601",
                LocalTime.of(19, 0),
                LocalTime.of(21, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(99L).classType()).isEqualTo(ClassType.CLASS);
        assertThat(request.toCommand(99L).classroomCode()).isEqualTo("601");
        assertThat(request.toCommand(99L).teacherId()).isNull();
        assertThat(request.toCommand(99L).teacherName()).isNull();
        assertThat(request.toCommand(99L).schedules())
                .containsExactly(new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)));
    }

    @Test
    void acceptsMultipleSchedulesWithoutLegacySingleScheduleFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "Math regular",
                ClassType.CLASS,
                null,
                "601",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new ScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)),
                        new ScheduleRequest(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)),
                        new ScheduleRequest(DayOfWeek.FRIDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))));

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(99L).schedules())
                .containsExactly(
                        new ScheduleInput(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)),
                        new ScheduleInput(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)),
                        new ScheduleInput(DayOfWeek.FRIDAY, LocalTime.of(19, 0), LocalTime.of(21, 0)));
    }

    @Test
    void rejectsMissingScheduleInput() {
        CreateLectureRequest request = new CreateLectureRequest(
                "Math regular",
                ClassType.CLASS,
                null,
                "601",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
