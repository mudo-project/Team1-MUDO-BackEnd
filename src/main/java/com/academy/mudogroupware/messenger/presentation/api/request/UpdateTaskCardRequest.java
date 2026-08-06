package com.academy.mudogroupware.messenger.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.messenger.application.command.UpdateTaskCardCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateTaskCardRequest(
        @NotBlank String content,
        LocalDate dueDate,
        @NotEmpty List<@NotNull @Positive Long> assigneeIds
) {

    public UpdateTaskCardCommand toCommand(Long chatRoomId, Long cardId, Long requesterId) {
        return new UpdateTaskCardCommand(chatRoomId, cardId, requesterId, content, dueDate, assigneeIds);
    }
}
