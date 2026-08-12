package com.academy.mudogroupware.users.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UpdateMemberProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidEmailFormat() {
        UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(null, null, "invalid-email", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void allowsNullEmailForPartialUpdate() {
        UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
