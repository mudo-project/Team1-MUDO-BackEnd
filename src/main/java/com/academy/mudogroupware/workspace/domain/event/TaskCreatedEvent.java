package com.academy.mudogroupware.workspace.domain.event;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 업무 생성 실시간 브로드캐스트 이벤트
public record TaskCreatedEvent(
    Long workspaceId,
    Long taskId,
    String title,
    TaskStatus status,
    LocalDate dueAt,
    Long createdBy,
    LocalDateTime createdAt) {}
