package com.academy.mudogroupware.workspace.application.command.task;

public record DeleteTaskCommand(Long workspaceId, Long taskId, Long requesterId) {}
