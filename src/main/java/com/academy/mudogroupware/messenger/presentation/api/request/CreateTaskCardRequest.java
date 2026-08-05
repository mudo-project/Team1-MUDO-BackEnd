package com.academy.mudogroupware.messenger.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTaskCardRequest(
        @NotBlank String content,
        LocalDate dueDate,
        @NotEmpty List<@NotNull @Positive Long> assigneeIds
) {

    public CreateTaskCardCommand toCommand(Long chatRoomId, Long assignerId) {
        return new CreateTaskCardCommand(chatRoomId, assignerId, content, dueDate, assigneeIds);
    }
}
