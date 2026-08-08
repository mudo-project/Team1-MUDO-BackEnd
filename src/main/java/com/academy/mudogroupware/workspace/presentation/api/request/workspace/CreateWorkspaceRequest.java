package com.academy.mudogroupware.workspace.presentation.api.request.workspace;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.workspace.CreateWorkspaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateWorkspaceRequest(
    @Schema(description = "워크스페이스 이름", example = "8월 학사 운영")
        @NotBlank(message = "워크스페이스 이름은 필수입니다.")
        @Size(max = 100, message = "워크스페이스 이름은 100자 이하여야 합니다.")
        String name,
    @Schema(description = "추가 참여자 사용자 번호 목록. 생성자는 자동 참여합니다.", example = "[12, 25]")
        List<
                @NotNull(message = "참여자 번호는 필수입니다.")
                @Positive(message = "참여자 번호는 양수여야 합니다.")
                Long>
            memberIds
) {

  public CreateWorkspaceCommand toCommand(AuthUser authUser) {
    return new CreateWorkspaceCommand(authUser.academyId(), authUser.userId(), name, memberIds);
  }
}
