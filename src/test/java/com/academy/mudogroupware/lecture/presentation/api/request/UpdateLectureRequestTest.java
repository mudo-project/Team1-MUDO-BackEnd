package com.academy.mudogroupware.lecture.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.model.ClassType;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UpdateLectureRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void convertsRequestToUpdateCommand() {
        UpdateLectureRequest request = new UpdateLectureRequest(
                "고2 수학 특강",
                ClassType.SPECIAL,
                DayOfWeek.TUESDAY,
                "B201",
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null,
                "김선생",
                "수학",
                "2026 여름방학",
                null,
                null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.toCommand(1L, 99L).lectureId()).isEqualTo(1L);
        assertThat(request.toCommand(1L, 99L).requesterId()).isEqualTo(99L);
        assertThat(request.toCommand(1L, 99L).schedules())
                .containsExactly(new ScheduleInput(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }
}
