package com.academy.mudogroupware.workspace.presentation.api.request;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.RenameWorkspaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameWorkspaceRequest(
    @Schema(description = "새 워크스페이스 이름", example = "9월 학사 운영")
        @NotBlank(message = "워크스페이스 이름은 필수입니다.")
        @Size(max = 100, message = "워크스페이스 이름은 100자 이하여야 합니다.")
        String name
) {

  public RenameWorkspaceCommand toCommand(AuthUser authUser, Long workspaceId) {
    return new RenameWorkspaceCommand(authUser.userId(), workspaceId, name);
  }
}
