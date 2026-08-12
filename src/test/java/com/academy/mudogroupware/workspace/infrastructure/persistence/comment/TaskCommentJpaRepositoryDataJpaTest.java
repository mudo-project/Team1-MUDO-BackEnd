package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.academy.mudogroupware.workspace.infrastructure.persistence.WorkspacePersistenceTestConfig;
import com.academy.mudogroupware.workspace.infrastructure.persistence.comment.TaskCommentJpaRepository.TaskCommentSummaryRow;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(WorkspacePersistenceTestConfig.class)
class TaskCommentJpaRepositoryDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TaskCommentJpaRepository taskCommentJpaRepository;

  @Test
  void summarizeByTaskIdsCountsCompletedAndTotalCommentsPerTaskAndSkipsTasksWithoutComments() {
    insertWorkspace(1L);
    insertTask(101L, 1L);
    insertTask(102L, 1L);
    insertComment(1L, 101L, true);
    insertComment(2L, 101L, false);
    insertComment(3L, 102L, false);

    List<TaskCommentSummaryRow> result =
        taskCommentJpaRepository.summarizeByTaskIds(List.of(101L, 102L, 103L));

    assertThat(result)
        .extracting(
            TaskCommentSummaryRow::getTaskId,
            TaskCommentSummaryRow::getCompletedCount,
            TaskCommentSummaryRow::getTotalCount)
        .containsExactlyInAnyOrder(tuple(101L, 1L, 2L), tuple(102L, 0L, 1L));
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, name, created_by, created_at, updated_at) "
            + "values (?, 'ws', 10, ?, ?)",
        workspaceId,
        at(),
        at());
  }

  private void insertTask(long taskId, long workspaceId) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, created_by, created_at, updated_at) "
            + "values (?, ?, 't', 'IN_PROGRESS', 10, ?, ?)",
        taskId,
        workspaceId,
        at(),
        at());
  }

  private void insertComment(long commentId, long taskId, boolean completed) {
    jdbcTemplate.update(
        "insert into task_comment (comment_id, task_id, content, is_completed, author_id, created_at, updated_at) "
            + "values (?, ?, 'c', ?, 10, ?, ?)",
        commentId,
        taskId,
        completed,
        at(),
        at());
  }

  private LocalDateTime at() {
    return LocalDateTime.of(2026, 8, 1, 9, 0);
  }
}
