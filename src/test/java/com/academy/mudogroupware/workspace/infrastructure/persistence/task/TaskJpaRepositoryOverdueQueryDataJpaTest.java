package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TimeConfig.class)
class TaskJpaRepositoryOverdueQueryDataJpaTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TaskJpaRepository taskJpaRepository;

  @Test
  void findOverdueRegularTasksExcludesCompletedDelayedAndNotYetDueTasks() {
    insertWorkspace(1L, null);
    insertRegularTask(1L, 1L, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 4));
    insertRegularTask(2L, 1L, TaskStatus.COMPLETED, LocalDate.of(2026, 8, 4));
    insertRegularTask(3L, 1L, TaskStatus.DELAYED, LocalDate.of(2026, 8, 4));
    insertRegularTask(4L, 1L, TaskStatus.WAITING, LocalDate.of(2026, 8, 5));
    insertRegularTask(5L, 1L, TaskStatus.WAITING, null);

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRegularTasks(TODAY, TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findOverdueRegularTasksExcludesTasksInSoftDeletedWorkspace() {
    insertWorkspace(1L, null);
    insertWorkspace(2L, at(2026, 8, 4));
    insertRegularTask(1L, 1L, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 4));
    insertRegularTask(2L, 2L, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 4));

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRegularTasks(TODAY, TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findOverdueRecurringTasksExcludesCompletedDelayedAndNotYetDueOccurrences() {
    insertWorkspace(1L, null);
    insertRecurringTemplate(100L, 1L);
    insertRecurringTask(1L, 1L, 100L, TaskStatus.WAITING, LocalDateTime.of(2026, 8, 3, 9, 0));
    insertRecurringTask(2L, 1L, 100L, TaskStatus.COMPLETED, LocalDateTime.of(2026, 8, 3, 10, 0));
    insertRecurringTask(3L, 1L, 100L, TaskStatus.DELAYED, LocalDateTime.of(2026, 8, 3, 11, 0));
    insertRecurringTask(4L, 1L, 100L, TaskStatus.WAITING, at(2026, 8, 5));
    // 자정 정각 경계: startOfToday와 정확히 같은 값은 "< startOfToday"에 포함되지 않아야 한다(<=로 회귀하면 이 케이스가 실패한다)
    insertRecurringTask(5L, 1L, 100L, TaskStatus.WAITING, TODAY.atStartOfDay());

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRecurringTasks(
            TODAY.atStartOfDay(), TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findOverdueRecurringTasksExcludesTasksInSoftDeletedWorkspace() {
    insertWorkspace(1L, null);
    insertWorkspace(2L, at(2026, 8, 4));
    insertRecurringTemplate(100L, 1L);
    insertRecurringTemplate(200L, 2L);
    insertRecurringTask(1L, 1L, 100L, TaskStatus.WAITING, LocalDateTime.of(2026, 8, 3, 9, 0));
    insertRecurringTask(2L, 2L, 200L, TaskStatus.WAITING, LocalDateTime.of(2026, 8, 3, 9, 0));

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRecurringTasks(
            TODAY.atStartOfDay(), TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  private void insertWorkspace(long workspaceId, LocalDateTime deletedAt) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at, deleted_at) "
            + "values (?, 1, ?, 10, ?, ?, ?)",
        workspaceId,
        "ws" + workspaceId,
        at(2026, 8, 1),
        at(2026, 8, 1),
        deletedAt);
  }

  private void insertRegularTask(long taskId, long workspaceId, TaskStatus status, LocalDate dueAt) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, ?, 't', ?, ?, 10, ?, ?)",
        taskId,
        workspaceId,
        status.name(),
        dueAt,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRecurringTemplate(long templateId, long workspaceId) {
    jdbcTemplate.update(
        "insert into recurring_task_template "
            + "(recurring_template_id, workspace_id, title, recurrence_type, recurrence_rule, created_by, created_at, updated_at) "
            + "values (?, ?, 'weekly', 'WEEKLY', '{}', 10, ?, ?)",
        templateId,
        workspaceId,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRecurringTask(
      long taskId, long workspaceId, long templateId, TaskStatus status, LocalDateTime scheduledFor) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, ?, ?, 't', ?, ?, 10, ?, ?)",
        taskId,
        workspaceId,
        templateId,
        status.name(),
        scheduledFor,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private LocalDateTime at(int year, int month, int day) {
    return LocalDateTime.of(year, month, day, 9, 0);
  }
}
