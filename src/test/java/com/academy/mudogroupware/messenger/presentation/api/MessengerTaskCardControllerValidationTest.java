package com.academy.mudogroupware.messenger.presentation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.messenger.application.query.TaskCardRole;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class MessengerTaskCardControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOversizedMyTaskCardPageSize() throws NoSuchMethodException {
        MessengerTaskCardController controller = new MessengerTaskCardController(mock(TaskCardQueryUseCase.class));
        Method method = MessengerTaskCardController.class.getMethod(
                "getMyTaskCards", AuthUser.class, TaskCardRole.class, LocalDateTime.class, Long.class, int.class);

        Set<ConstraintViolation<MessengerTaskCardController>> violations =
                validator.forExecutables().validateParameters(
                        controller, method, new Object[] {null, TaskCardRole.SENT, null, null, 101});

        assertThat(violations).isNotEmpty();
    }
}
