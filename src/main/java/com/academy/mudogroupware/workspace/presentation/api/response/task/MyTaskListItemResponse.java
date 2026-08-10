package com.academy.mudogroupware.workspace.presentation.api.response.task;

import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MyTaskListItemResponse(
    @Schema(description = "업무 번호", example = "101") Long taskId,
    @Schema(description = "워크스페이스 번호", example = "1") Long workspaceId,
    @Schema(description = "워크스페이스 이름", example = "8월 학사 운영") String workspaceName,
    @Schema(description = "업무 제목", example = "9월 시간표 초안 작성") String title,
    @Schema(description = "기한", example = "2026-08-10") LocalDate dueAt,
    @Schema(description = "상태", example = "WAITING") TaskStatus status) {

  public static MyTaskListItemResponse from(MyTaskListItem item) {
    return MyTaskListItemResponse.builder()
        .taskId(item.taskId())
        .workspaceId(item.workspaceId())
        .workspaceName(item.workspaceName())
        .title(item.title())
        .dueAt(item.dueAt())
        .status(item.status())
        .build();
  }
}
