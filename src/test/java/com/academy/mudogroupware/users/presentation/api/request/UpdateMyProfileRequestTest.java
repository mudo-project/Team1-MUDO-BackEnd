package com.academy.mudogroupware.users.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UpdateMyProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidEmailFormat() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(null, "invalid-email");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void allowsNullEmailForPartialUpdate() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(null, null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
