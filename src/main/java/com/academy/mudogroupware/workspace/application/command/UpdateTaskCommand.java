package com.academy.mudogroupware.workspace.application.command;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;

// status와 dueAt은 각각 선택이다. 둘 다 null인 요청은 Presentation 계층에서 400으로 걸러진다.
public record UpdateTaskCommand(
    Long workspaceId, Long taskId, Long requesterId, TaskStatus status, LocalDate dueAt) {}
