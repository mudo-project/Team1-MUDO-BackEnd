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
    // academyId는 users 도메인이 Phase 2(academyId 스코핑 제거)를 진행하면서 더 이상 쓰지 않는
    // 죽은 파라미터가 됐다(REVISION.md 참고). Command/Service/Port 시그니처까지 걷어내는 정리는
    // workspace 담당자의 후속 작업으로 남겨둔다.
    return new AddWorkspaceMembersCommand(
        null, authUser.userId(), workspaceId, memberIds);
  }
}
