package com.academy.mudogroupware.workspace.presentation.api.response.task;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.academy.mudogroupware.workspace.application.query.task.TaskDetail;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.presentation.api.response.workspace.WorkspaceDetailMemberResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TaskDetailResponse(
    @Schema(description = "업무 번호", example = "101") Long taskId,
    @Schema(description = "업무 제목", example = "성적 데이터 7월분 엑셀 정리") String title,
    @Schema(description = "등록자") WorkspaceDetailMemberResponse creator,
    @Schema(description = "등록일시", example = "2026-07-29T09:30:00") LocalDateTime createdAt,
    @Schema(description = "상태", example = "IN_PROGRESS") TaskStatus status,
    @Schema(description = "기한", example = "2026-08-05") LocalDate dueAt,
    @JsonInclude(NON_NULL)
        @Schema(description = "최종 상태 변경일시(이력 없으면 생략)", example = "2026-08-02T09:00:00")
        LocalDateTime lastStatusChangedAt) {

  public static TaskDetailResponse from(TaskDetail detail) {
    return TaskDetailResponse.builder()
        .taskId(detail.taskId())
        .title(detail.title())
        .creator(
            WorkspaceDetailMemberResponse.builder()
                .userId(detail.creator().userId())
                .name(detail.creator().name())
                .build())
        .createdAt(detail.createdAt())
        .status(detail.status())
        .dueAt(detail.dueAt())
        .lastStatusChangedAt(detail.lastStatusChangedAt())
        .build();
  }
}
