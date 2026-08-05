package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.application.query.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskCommentJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, WorkspaceDetailQueryAdapter.class})
class WorkspaceDetailQueryAdapterDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private WorkspaceDetailQueryAdapter adapter;
  @Autowired private TaskJpaRepository taskJpaRepository;
  @Autowired private TaskCommentJpaRepository taskCommentJpaRepository;

  @Test
  void assemblesNameMembersTasksAndCommentSummariesForTheSelectedDate() {
    insertWorkspace(1L);
    insertMember(1L, 10L);
    insertTask(101L, 1L, "업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7), 10L);
    insertComment(1L, 101L, true);
    insertComment(2L, 101L, false);

    assertThat(adapter.findActiveWorkspaceName(1L)).contains("ws");
    assertThat(adapter.findMemberIds(1L)).containsExactly(10L);

    List<WorkspaceTaskCandidate> tasks =
        adapter.findVisibleTasks(1L, LocalDate.of(2026, 8, 5));
    assertThat(tasks).extracting(WorkspaceTaskCandidate::taskId).containsExactly(101L);

    List<TaskCommentSummary> summaries =
        adapter.findCommentSummaries(List.of(101L));
    assertThat(summaries)
        .extracting(TaskCommentSummary::taskId, TaskCommentSummary::completedCount, TaskCommentSummary::totalCount)
        .containsExactly(org.assertj.core.groups.Tuple.tuple(101L, 1L, 2L));
  }

  @Test
  void findCommentSummariesReturnsEmptyListForEmptyTaskIds() {
    assertThat(adapter.findCommentSummaries(List.of())).isEmpty();
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at) "
            + "values (?, 1, 'ws', 10, ?, ?)",
        workspaceId,
        at(),
        at());
  }

  private void insertMember(long workspaceId, long userId) {
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId,
        userId,
        at());
  }

  private void insertTask(
      long taskId, long workspaceId, String title, TaskStatus status, LocalDate dueAt, long createdBy) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
        taskId,
        workspaceId,
        title,
        status.name(),
        dueAt,
        createdBy,
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
