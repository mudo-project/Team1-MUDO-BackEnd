package com.academy.mudogroupware.messenger.presentation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.messenger.application.usecase.ChatMessageQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomMemberQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.ChatRoomQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CompleteTaskUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CreateChatRoomUseCase;
import com.academy.mudogroupware.messenger.application.usecase.CreateTaskCardUseCase;
import com.academy.mudogroupware.messenger.application.usecase.DeleteMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.DeleteTaskCardUseCase;
import com.academy.mudogroupware.messenger.application.usecase.SendMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.TaskCardQueryUseCase;
import com.academy.mudogroupware.messenger.application.usecase.UpdateMessageUseCase;
import com.academy.mudogroupware.messenger.application.usecase.UpdateTaskCardUseCase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class MessengerControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOversizedMessagePageSize() throws NoSuchMethodException {
        MessengerController controller = new MessengerController(
                mock(CreateChatRoomUseCase.class),
                mock(ChatRoomQueryUseCase.class),
                mock(ChatRoomMemberQueryUseCase.class),
                mock(SendMessageUseCase.class),
                mock(UpdateMessageUseCase.class),
                mock(DeleteMessageUseCase.class),
                mock(ChatMessageQueryUseCase.class),
                mock(CreateTaskCardUseCase.class),
                mock(TaskCardQueryUseCase.class),
                mock(CompleteTaskUseCase.class),
                mock(UpdateTaskCardUseCase.class),
                mock(DeleteTaskCardUseCase.class));
        Method method = MessengerController.class.getMethod(
                "getMessages", AuthUser.class, Long.class, java.time.LocalDateTime.class, Long.class, int.class);

        Set<ConstraintViolation<MessengerController>> violations =
                validator.forExecutables().validateParameters(
                        controller, method, new Object[] {null, 1L, null, null, 101});

        assertThat(violations).isNotEmpty();
    }
}
