package com.academy.mudogroupware.workspace.presentation.api.request.comment;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateTaskCommentRequest(
    @Schema(description = "댓글 내용", example = "확인 부탁드립니다")
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content,
    @Schema(description = "멘션할 워크스페이스 참여자 ID 목록", example = "[11, 12]")
        List<Long> mentionedUserIds
) {

  public CreateTaskCommentRequest {
    content = content == null ? null : content.trim();
    mentionedUserIds = mentionedUserIds == null ? List.of() : mentionedUserIds;
  }

  public CreateTaskCommentCommand toCommand(AuthUser authUser, Long workspaceId, Long taskId) {
    return new CreateTaskCommentCommand(workspaceId, taskId, authUser.userId(), content, mentionedUserIds);
  }
}
