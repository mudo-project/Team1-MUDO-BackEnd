package com.academy.mudogroupware.workspace.application.command.task;

import java.time.LocalDate;

public record CreateTaskCommand(Long workspaceId, Long requesterId, String title, LocalDate dueAt) {}
