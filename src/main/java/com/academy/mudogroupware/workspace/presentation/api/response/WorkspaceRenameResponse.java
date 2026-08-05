package com.academy.mudogroupware.workspace.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record WorkspaceRenameResponse(
    @Schema(description = "워크스페이스 번호", example = "1") Long workspaceId,
    @Schema(description = "수정된 워크스페이스 이름", example = "9월 학사 운영") String name
) {

  public static WorkspaceRenameResponse from(Long workspaceId, String name) {
    return WorkspaceRenameResponse.builder().workspaceId(workspaceId).name(name).build();
  }
}
