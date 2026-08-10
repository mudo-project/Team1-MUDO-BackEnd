package com.academy.mudogroupware.workspace.application.query.task;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskDetail(
    Long taskId,
    String title,
    WorkspaceMemberInfo creator,
    LocalDateTime createdAt,
    TaskStatus status,
    LocalDate dueAt,
    LocalDateTime lastStatusChangedAt) {}
