package com.academy.mudogroupware.workspace.presentation.api.response;

import com.academy.mudogroupware.workspace.application.query.WorkspaceMemberInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record WorkspaceDetailMemberResponse(
    @Schema(description = "사용자 번호", example = "12") Long userId,
    @Schema(description = "표시 이름", example = "김지수") String name) {

  public static WorkspaceDetailMemberResponse from(WorkspaceMemberInfo info) {
    return new WorkspaceDetailMemberResponse(info.userId(), info.name());
  }
}
