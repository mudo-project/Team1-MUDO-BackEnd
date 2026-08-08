package com.academy.mudogroupware.workspace.presentation.api.response.workspace;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record WorkspaceDetailResponse(
    @Schema(description = "워크스페이스 번호", example = "1")
    Long workspaceId,

    @Schema(description = "워크스페이스 이름", example = "1월 학사 운영")
    String name,

    @Schema(description = "참여자 수", example = "3")
    long memberCount,

    @Schema(description = "참여자 목록")
    List<WorkspaceDetailMemberResponse> members,

    @Schema(description = "표시되는 업무 카드 수", example = "7")
    long taskCount,

    @Schema(description = "업무 카드 목록")
    List<WorkspaceDetailTaskResponse> tasks
) {

  public static WorkspaceDetailResponse from(WorkspaceDetail detail) {
    return WorkspaceDetailResponse.builder()
        .workspaceId(detail.workspaceId())
        .name(detail.name())
        .memberCount(detail.memberCount())
        .members(detail.members().stream().map(WorkspaceDetailMemberResponse::from).toList())
        .taskCount(detail.taskCount())
        .tasks(detail.tasks().stream().map(WorkspaceDetailTaskResponse::from).toList())
        .build();
  }
}
