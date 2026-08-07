package com.academy.mudogroupware.workspace.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskCommentTest {

  private static final long TASK_ID = 101L;
  private static final long AUTHOR_ID = 10L;
  private static final long MENTIONED_ID = 20L;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 10, 0);

  @Test
  void createsWithMentionsAndNoCompletion() {
    TaskComment comment =
        TaskComment.create(TASK_ID, AUTHOR_ID, "  확인 부탁드립니다  ", List.of(MENTIONED_ID), NOW);

    assertThat(comment.getId()).isNull();
    assertThat(comment.getTaskId()).isEqualTo(TASK_ID);
    assertThat(comment.getAuthorId()).isEqualTo(AUTHOR_ID);
    assertThat(comment.getContent()).isEqualTo("확인 부탁드립니다");
    assertThat(comment.isCompleted()).isFalse();
    assertThat(comment.getCompletedBy()).isNull();
    assertThat(comment.getCompletedAt()).isNull();
    assertThat(comment.getMentions()).extracting(TaskCommentMention::getMentionedUserId)
        .containsExactly(MENTIONED_ID);
    assertThat(comment.getCreatedAt()).isEqualTo(NOW);
    assertThat(comment.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void updateContentReplacesContentAndMentions() {
    TaskComment comment = TaskComment.create(TASK_ID, AUTHOR_ID, "원본", List.of(MENTIONED_ID), NOW);
    LocalDateTime later = NOW.plusHours(1);

    TaskComment updated = comment.updateContent("  수정됨  ", List.of(30L, 31L), later);

    assertThat(updated.getContent()).isEqualTo("수정됨");
    assertThat(updated.getMentions()).extracting(TaskCommentMention::getMentionedUserId)
        .containsExactly(30L, 31L);
    assertThat(updated.getUpdatedAt()).isEqualTo(later);
    assertThat(updated.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void toggleCompleteMarksCompletedThenCancels() {
    TaskComment comment = TaskComment.create(TASK_ID, AUTHOR_ID, "내용", List.of(), NOW);
    LocalDateTime completedTime = NOW.plusMinutes(5);

    TaskComment completed = comment.toggleComplete(AUTHOR_ID, completedTime);
    assertThat(completed.isCompleted()).isTrue();
    assertThat(completed.getCompletedBy()).isEqualTo(AUTHOR_ID);
    assertThat(completed.getCompletedAt()).isEqualTo(completedTime);

    LocalDateTime canceledTime = completedTime.plusMinutes(1);
    TaskComment canceled = completed.toggleComplete(99L, canceledTime);
    assertThat(canceled.isCompleted()).isFalse();
    assertThat(canceled.getCompletedBy()).isNull();
    assertThat(canceled.getCompletedAt()).isNull();
  }

  @Test
  void belongsToChecksTaskId() {
    TaskComment comment = TaskComment.create(TASK_ID, AUTHOR_ID, "내용", List.of(), NOW);

    assertThat(comment.belongsTo(TASK_ID)).isTrue();
    assertThat(comment.belongsTo(999L)).isFalse();
  }

  @Test
  void createRejectsBlankContent() {
    assertThatThrownBy(() -> TaskComment.create(TASK_ID, AUTHOR_ID, "   ", List.of(), NOW))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void updateContentRejectsBlankContent() {
    TaskComment comment = TaskComment.create(TASK_ID, AUTHOR_ID, "원본", List.of(), NOW);

    assertThatThrownBy(() -> comment.updateContent("   ", List.of(), NOW.plusHours(1)))
        .isInstanceOf(BadRequestException.class);
  }
}
