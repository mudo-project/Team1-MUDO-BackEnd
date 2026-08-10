package com.academy.mudogroupware.rollcall.presentation.api.request;

import com.academy.mudogroupware.rollcall.application.command.CreateMessageTemplateCommand;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMessageTemplateRequest(
        @NotBlank String name,
        @NotNull AttendanceStatus status,
        @NotBlank String content
) {

    public CreateMessageTemplateCommand toCommand(Long createdBy) {
        return new CreateMessageTemplateCommand(name, status, content, createdBy);
    }
}
