package com.academy.mudogroupware.users.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateAccountRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidEmailFormat() {
        CreateAccountRequest request = new CreateAccountRequest("teacher01", "김강사", null, "invalid-email", 5L);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void allowsNullEmail() {
        CreateAccountRequest request = new CreateAccountRequest("teacher01", "김강사", null, null, 5L);

        assertThat(validator.validate(request)).isEmpty();
    }
}
