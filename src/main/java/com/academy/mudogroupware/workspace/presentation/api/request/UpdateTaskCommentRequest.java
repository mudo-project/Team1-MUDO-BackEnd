package com.academy.mudogroupware.workspace.presentation.api.request;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateTaskCommentRequest(
    @Schema(description = "수정할 댓글 내용", example = "확인했습니다")
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content,
    @Schema(description = "교체할 멘션 대상 ID 목록. 비우면 멘션이 모두 제거됩니다.", example = "[11]")
        List<Long> mentionedUserIds
) {

  public UpdateTaskCommentRequest {
    content = content == null ? null : content.trim();
    mentionedUserIds = mentionedUserIds == null ? List.of() : mentionedUserIds;
  }

  public UpdateTaskCommentCommand toCommand(
      AuthUser authUser, Long workspaceId, Long taskId, Long commentId) {
    return new UpdateTaskCommentCommand(
        workspaceId, taskId, commentId, authUser.userId(), content, mentionedUserIds);
  }
}
