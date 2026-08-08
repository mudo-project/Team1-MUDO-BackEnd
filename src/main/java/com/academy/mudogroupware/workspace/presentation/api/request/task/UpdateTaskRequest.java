package com.academy.mudogroupware.workspace.presentation.api.request.task;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record UpdateTaskRequest(
    @Schema(description = "변경할 업무 상태", example = "IN_PROGRESS") TaskStatus status,
    @Schema(description = "새 마감일", example = "2026-08-20") LocalDate dueAt
) {

  @AssertTrue(message = "상태와 마감일 중 최소 하나는 입력해야 합니다.")
  public boolean isAtLeastOneFieldPresent() {
    return status != null || dueAt != null;
  }

  public UpdateTaskCommand toCommand(AuthUser authUser, Long workspaceId, Long taskId) {
    return new UpdateTaskCommand(workspaceId, taskId, authUser.userId(), status, dueAt);
  }
}
