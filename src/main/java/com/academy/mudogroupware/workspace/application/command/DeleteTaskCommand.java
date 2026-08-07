package com.academy.mudogroupware.workspace.application.command;

public record DeleteTaskCommand(Long workspaceId, Long taskId, Long requesterId) {}
