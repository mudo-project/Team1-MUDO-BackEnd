package com.academy.mudogroupware.workspace.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateWorkspaceResponse(
    @Schema(description = "생성된 워크스페이스 번호", example = "1")
    Long workspaceId
) {

  public static CreateWorkspaceResponse from(Long workspaceId) {
    return CreateWorkspaceResponse.builder()
        .workspaceId(workspaceId)
        .build();
  }
}
