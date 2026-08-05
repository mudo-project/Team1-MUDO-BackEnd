package com.academy.mudogroupware.workspace.presentation.api.response;

import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import io.swagger.v3.oas.annotations.media.Schema;

public record WorkspaceListResponse(
    @Schema(description = "워크스페이스 번호", example = "1") Long workspaceId,
    @Schema(description = "워크스페이스 이름", example = "8월 학사 운영") String name,
    @Schema(description = "워크스페이스 참여자 수", example = "3") long memberCount) {

  public static WorkspaceListResponse from(WorkspaceListItem item) {
    return new WorkspaceListResponse(item.workspaceId(), item.name(), item.memberCount());
  }
}
