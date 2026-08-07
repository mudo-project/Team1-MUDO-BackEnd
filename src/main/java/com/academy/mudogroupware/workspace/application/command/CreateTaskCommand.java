package com.academy.mudogroupware.workspace.application.command;

import java.time.LocalDate;

public record CreateTaskCommand(Long workspaceId, Long requesterId, String title, LocalDate dueAt) {}
