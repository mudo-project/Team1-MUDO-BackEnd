package com.academy.mudogroupware.rollcall.application.command;

public record UpdateMessageTemplateCommand(
        Long templateId,
        Long academyId,
        String name,
        String content
) {
}
