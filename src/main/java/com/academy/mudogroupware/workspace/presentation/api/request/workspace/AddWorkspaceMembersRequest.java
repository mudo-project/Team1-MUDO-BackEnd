package com.academy.mudogroupware.workspace.presentation.api.request.workspace;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.workspace.AddWorkspaceMembersCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record AddWorkspaceMembersRequest(
    @Schema(description = "추가할 참여자 사용자 번호 목록", example = "[12, 25]")
        @NotEmpty(message = "참여자 번호 목록은 필수입니다.")
        List<
                @NotNull(message = "참여자 번호는 필수입니다.")
                @Positive(message = "참여자 번호는 양수여야 합니다.")
                Long>
            memberIds
) {

  public AddWorkspaceMembersCommand toCommand(AuthUser authUser, Long workspaceId) {
    return new AddWorkspaceMembersCommand(
        authUser.academyId(), authUser.userId(), workspaceId, memberIds);
  }
}
