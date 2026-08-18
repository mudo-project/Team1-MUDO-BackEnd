package com.academy.mudogroupware.workspace.domain.event;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;

// 업무 상태/마감일 변경 실시간 브로드캐스트 이벤트. PATCH가 status/dueAt 중 뭘 바꿨든 동일하게 발행한다.
public record TaskUpdatedEvent(Long workspaceId, Long taskId, TaskStatus status, LocalDate dueAt) {}
