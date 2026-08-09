package com.academy.mudogroupware.workspace.application.command.task;

import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import java.util.Map;

public record CreateRecurringTaskTemplateCommand(
    Long workspaceId,
    Long requesterId,
    String title,
    RecurrenceType recurrenceType,
    Map<String, Object> recurrenceRule) {}
