package com.academy.mudogroupware.workspace.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record WorkspaceRecoverResponse(
    @Schema(description = "워크스페이스 번호", example = "1") Long workspaceId,
    @Schema(description = "복구된 워크스페이스 이름", example = "개발팀(20260806153012)") String name
) {

  public static WorkspaceRecoverResponse from(Long workspaceId, String name) {
    return WorkspaceRecoverResponse.builder().workspaceId(workspaceId).name(name).build();
  }
}
