package com.academy.mudogroupware.workspace.presentation.api.response.workspace;

import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record WorkspaceDetailTaskResponse(
    @Schema(description = "업무 번호", example = "101")
    Long taskId,

    @Schema(description = "업무 제목", example = "8월 원생 청구서 발송")
    String title,

    @Schema(description = "업무 상태")
    TaskStatus status,

    @Schema(description = "생성자")
    WorkspaceDetailMemberResponse creator,

    @Schema(description = "기한. 반복 업무는 항상 null", example = "2026-08-07")
    LocalDate dueAt,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "완료 코멘트 수. 코멘트가 없으면 응답에서 제외", example = "1")
    Long completedCommentCount,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "전체 코멘트 수. 코멘트가 없으면 응답에서 제외", example = "2")
    Long commentCount
) {

  public static WorkspaceDetailTaskResponse from(WorkspaceTaskItem item) {
    return WorkspaceDetailTaskResponse.builder()
        .taskId(item.taskId())
        .title(item.title())
        .status(item.status())
        .creator(WorkspaceDetailMemberResponse.from(item.creator()))
        .dueAt(item.dueAt())
        .completedCommentCount(item.completedCommentCount())
        .commentCount(item.commentCount())
        .build();
  }
}
