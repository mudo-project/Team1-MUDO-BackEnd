package com.academy.mudogroupware.attendance.presentation.api.request;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SaveAttendancePolicyRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void rejectsNullElementInWeekdays() {
        SaveAttendancePolicyRequest request = new SaveAttendancePolicyRequest(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                10,
                true,
                Collections.singletonList(null));

        var violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .startsWith("weekdays[0]")));
    }
}
