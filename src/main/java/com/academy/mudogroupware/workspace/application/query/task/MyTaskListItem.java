package com.academy.mudogroupware.workspace.application.query.task;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;

public record MyTaskListItem(
    Long taskId,
    Long workspaceId,
    String workspaceName,
    String title,
    LocalDate dueAt,
    TaskStatus status) {}
