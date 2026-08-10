package com.academy.mudogroupware.rollcall.application.command;

public record UpdateMessageTemplateCommand(
        Long templateId,
        String name,
        String content
) {
}
