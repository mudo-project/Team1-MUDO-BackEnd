package com.academy.mudogroupware.workspace.presentation.api.response.comment;

import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;
import com.academy.mudogroupware.workspace.presentation.api.response.workspace.WorkspaceDetailMemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TaskCommentListItemResponse(
    @Schema(description = "댓글 번호", example = "1") Long commentId,
    @Schema(description = "댓글 내용", example = "수학A반 완료") String content,
    @Schema(description = "작성자") WorkspaceDetailMemberResponse author,
    @Schema(description = "완료 여부", example = "true") boolean completed,
    @Schema(description = "생성일시", example = "2026-08-01T16:00:00") LocalDateTime createdAt) {

  public static TaskCommentListItemResponse from(TaskCommentListItem item) {
    return TaskCommentListItemResponse.builder()
        .commentId(item.commentId())
        .content(item.content())
        .author(
            WorkspaceDetailMemberResponse.builder()
                .userId(item.author().userId())
                .name(item.author().name())
                .build())
        .completed(item.completed())
        .createdAt(item.createdAt())
        .build();
  }
}
