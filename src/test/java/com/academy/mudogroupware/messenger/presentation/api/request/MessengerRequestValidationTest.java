package com.academy.mudogroupware.messenger.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.messenger.application.command.CreateChatRoomCommand;
import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;
import com.academy.mudogroupware.messenger.application.command.UpdateTaskCardCommand;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class MessengerRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNullParticipantId() {
        CreateChatRoomRequest request = new CreateChatRoomRequest(Collections.singletonList(null), null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveAssigneeId() {
        CreateTaskCardRequest request = new CreateTaskCardRequest("task", null, List.of(0L));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsBlankUpdatedMessageContent() {
        UpdateMessageRequest request = new UpdateMessageRequest(" ");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void commandRejectsNullParticipantIdAsDomainException() {
        assertThatThrownBy(() -> new CreateChatRoomCommand(1L, Collections.singletonList(null), null))
                .isInstanceOf(MessengerException.class);
    }

    @Test
    void commandRejectsNullAssigneeIdAsDomainException() {
        assertThatThrownBy(() -> new CreateTaskCardCommand(1L, 1L, "task", null, Collections.singletonList(null)))
                .isInstanceOf(MessengerException.class);
    }

    @Test
    void rejectsNonPositiveAssigneeIdOnUpdate() {
        UpdateTaskCardRequest request = new UpdateTaskCardRequest("task", null, List.of(0L));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void updateCommandRejectsNullAssigneeIdAsDomainException() {
        assertThatThrownBy(() -> new UpdateTaskCardCommand(1L, 1L, 1L, "task", null,
                Collections.singletonList(null)))
                .isInstanceOf(MessengerException.class);
    }
}
