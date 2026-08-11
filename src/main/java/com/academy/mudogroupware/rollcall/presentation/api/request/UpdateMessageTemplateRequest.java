package com.academy.mudogroupware.rollcall.presentation.api.request;

import com.academy.mudogroupware.rollcall.application.command.UpdateMessageTemplateCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageTemplateRequest(
        @NotBlank String name,
        @NotBlank String content
) {

    public UpdateMessageTemplateCommand toCommand(Long templateId) {
        return new UpdateMessageTemplateCommand(templateId, name, content);
    }
}
