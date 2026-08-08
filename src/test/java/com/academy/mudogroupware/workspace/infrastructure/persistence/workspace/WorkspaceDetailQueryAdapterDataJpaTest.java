package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.comment.TaskCommentJpaRepository;
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
  void findActiveWorkspaceNameReturnsTheWorkspaceName() {
    insertWorkspace(1L);

    assertThat(adapter.findActiveWorkspaceName(1L)).contains("ws");
  }

  @Test
  void findMemberIdsReturnsAllMembersOfTheWorkspace() {
    insertWorkspace(1L);
    insertMember(1L, 10L);

    assertThat(adapter.findMemberIds(1L)).containsExactly(10L);
  }

  @Test
  void findVisibleTasksMergesRegularAndRecurringTasksForTheSelectedDate() {
    insertWorkspace(1L);
    insertTask(101L, 1L, "일반 업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7), 10L);
    insertRecurringTemplate(200L, 1L);
    insertRecurringTask(102L, 1L, 200L, TaskStatus.WAITING, at(2026, 8, 5), 10L);

    List<WorkspaceTaskCandidate> tasks = adapter.findVisibleTasks(1L, LocalDate.of(2026, 8, 5));

    assertThat(tasks)
        .extracting(WorkspaceTaskCandidate::taskId)
        .containsExactlyInAnyOrder(101L, 102L);
  }

  @Test
  void findVisibleTasksExcludesRecurringTaskNotScheduledForTheSelectedDate() {
    insertWorkspace(1L);
    insertRecurringTemplate(200L, 1L);
    insertRecurringTask(102L, 1L, 200L, TaskStatus.WAITING, at(2026, 8, 5), 10L);

    List<WorkspaceTaskCandidate> tasks = adapter.findVisibleTasks(1L, LocalDate.of(2026, 8, 6));

    assertThat(tasks).isEmpty();
  }

  @Test
  void findCommentSummariesReturnsCompletedAndTotalCountPerTask() {
    insertWorkspace(1L);
    insertTask(101L, 1L, "업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7), 10L);
    insertComment(1L, 101L, true);
    insertComment(2L, 101L, false);

    List<TaskCommentSummary> summaries = adapter.findCommentSummaries(List.of(101L));

    assertThat(summaries)
        .extracting(
            TaskCommentSummary::taskId,
            TaskCommentSummary::completedCount,
            TaskCommentSummary::totalCount)
        .containsExactly(tuple(101L, 1L, 2L));
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
      long taskId,
      long workspaceId,
      String title,
      TaskStatus status,
      LocalDate dueAt,
      long createdBy) {
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

  private void insertRecurringTemplate(long templateId, long workspaceId) {
    jdbcTemplate.update(
        "insert into recurring_task_template "
            + "(recurring_template_id, workspace_id, title, recurrence_type, recurrence_rule, is_active, created_by, created_at, updated_at) "
            + "values (?, ?, 'daily', 'DAILY', '{}', true, 10, ?, ?)",
        templateId,
        workspaceId,
        at(),
        at());
  }

  private void insertRecurringTask(
      long taskId,
      long workspaceId,
      long templateId,
      TaskStatus status,
      LocalDateTime scheduledFor,
      long createdBy) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, ?, ?, '반복 업무', ?, ?, ?, ?, ?)",
        taskId,
        workspaceId,
        templateId,
        status.name(),
        scheduledFor,
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

  private LocalDateTime at(int year, int month, int day) {
    return LocalDateTime.of(year, month, day, 9, 0);
  }
}
