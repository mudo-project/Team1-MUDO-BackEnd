package com.academy.mudogroupware.workspace.presentation.api.response.task;

import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TaskUpdateResponse(
    @Schema(description = "업무 번호", example = "101") Long taskId,
    @Schema(description = "반영된 업무 상태", example = "IN_PROGRESS") TaskStatus status,
    @Schema(description = "반영된 마감일. 반복 업무는 null", example = "2026-08-20") LocalDate dueAt) {

  public static TaskUpdateResponse from(Task task) {
    return TaskUpdateResponse.builder()
        .taskId(task.getId())
        .status(task.getStatus())
        .dueAt(task.getDueAt())
        .build();
  }
}
