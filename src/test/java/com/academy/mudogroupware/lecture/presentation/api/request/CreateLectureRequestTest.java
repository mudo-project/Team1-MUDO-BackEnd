package com.academy.mudogroupware.lecture.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.model.ClassType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateLectureRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsTimetableSlotShapeWithOnlyRequiredFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "고1 수학 정규반",
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
    void rejectsMissingRequiredTimetableSlotFields() {
        CreateLectureRequest request = new CreateLectureRequest(
                "고1 수학 정규반",
                null,
                null,
                " ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(5);
    }
}
