package com.academy.mudogroupware.workspace.application.command.task;

public record DeleteRecurringTaskTemplateCommand(Long workspaceId, Long templateId, Long requesterId) {}
