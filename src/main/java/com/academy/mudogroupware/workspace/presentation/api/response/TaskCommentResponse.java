package com.academy.mudogroupware.workspace.presentation.api.response;

import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record TaskCommentResponse(
    @Schema(description = "댓글 번호", example = "501") Long commentId,
    @Schema(description = "소속 업무 번호", example = "101") Long taskId,
    @Schema(description = "작성자 사용자 번호", example = "10") Long authorId,
    @Schema(description = "댓글 내용", example = "확인 부탁드립니다") String content,
    @Schema(description = "완료 여부", example = "false") boolean completed,
    @Schema(description = "완료 처리자 사용자 번호. 미완료면 null", example = "10") Long completedBy,
    @Schema(description = "완료 처리 시각. 미완료면 null") LocalDateTime completedAt,
    @Schema(description = "멘션된 참여자 ID 목록") List<Long> mentionedUserIds,
    @Schema(description = "작성 시각") LocalDateTime createdAt,
    @Schema(description = "수정 시각") LocalDateTime updatedAt) {

  public static TaskCommentResponse from(TaskComment comment) {
    return TaskCommentResponse.builder()
        .commentId(comment.getId())
        .taskId(comment.getTaskId())
        .authorId(comment.getAuthorId())
        .content(comment.getContent())
        .completed(comment.isCompleted())
        .completedBy(comment.getCompletedBy())
        .completedAt(comment.getCompletedAt())
        .mentionedUserIds(comment.getMentions().stream().map(m -> m.getMentionedUserId()).toList())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }
}
