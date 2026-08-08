package com.academy.mudogroupware.workspace.application.query.task;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkspaceTaskCandidate(
    Long taskId,
    String title,
    TaskStatus status,
    LocalDate dueAt,
    Long createdBy,
    LocalDateTime createdAt) {}
