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
class TaskJpaRepositoryDataJpaTest {

  private static final long WORKSPACE_ID = 1L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TaskJpaRepository taskJpaRepository;

  @Test
  void findVisibleRegularTasksAlwaysReturnsIncompleteRegularTasksRegardlessOfSelectedDate() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, "미완료 업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7));

    List<TaskJpaEntity> result =
        taskJpaRepository.findVisibleRegularTasks(
            WORKSPACE_ID, startOfDay(2099, 1, 1), endOfDay(2099, 1, 1), TaskStatus.COMPLETED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findVisibleRegularTasksReturnsCompletedTaskOnlyOnItsLastCompletedDate() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, "완료 업무", TaskStatus.COMPLETED, LocalDate.of(2026, 8, 5));
    insertStatusHistory(1L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, at(2026, 8, 5));

    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 5), endOfDay(2026, 8, 5), TaskStatus.COMPLETED))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(1L);
    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 6), endOfDay(2026, 8, 6), TaskStatus.COMPLETED))
        .isEmpty();
  }

  @Test
  void findVisibleRegularTasksUsesOnlyTheLastCompletedHistoryWhenTaskWasReopened() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, "재완료 업무", TaskStatus.COMPLETED, LocalDate.of(2026, 8, 5));
    insertStatusHistory(1L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, at(2026, 8, 3));
    insertStatusHistory(1L, TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS, at(2026, 8, 4));
    insertStatusHistory(1L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, at(2026, 8, 6));

    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 3), endOfDay(2026, 8, 3), TaskStatus.COMPLETED))
        .isEmpty();
    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 6), endOfDay(2026, 8, 6), TaskStatus.COMPLETED))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(1L);
  }

  @Test
  void findVisibleRecurringTasksReturnsOnlyTheOccurrenceScheduledForTheSelectedDate() {
    insertWorkspace(WORKSPACE_ID);
    insertRecurringTemplate(100L, WORKSPACE_ID);
    insertRecurringTask(1L, WORKSPACE_ID, 100L, TaskStatus.WAITING, at(2026, 8, 5));
    insertRecurringTask(2L, WORKSPACE_ID, 100L, TaskStatus.DELAYED, at(2026, 8, 3));

    assertThat(
            taskJpaRepository.findVisibleRecurringTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 5), endOfDay(2026, 8, 5)))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(1L);
    assertThat(
            taskJpaRepository.findVisibleRecurringTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 3), endOfDay(2026, 8, 3)))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(2L);
    assertThat(
            taskJpaRepository.findVisibleRecurringTasks(
                WORKSPACE_ID, startOfDay(2026, 8, 4), endOfDay(2026, 8, 4)))
        .isEmpty();
  }

  @Test
  void findVisibleRegularTasksExcludesTasksFromOtherWorkspaces() {
    long otherWorkspaceId = 2L;
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(otherWorkspaceId);
    insertTask(1L, WORKSPACE_ID, "내 워크스페이스 업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7));
    insertTask(2L, otherWorkspaceId, "다른 워크스페이스 업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7));

    List<TaskJpaEntity> result =
        taskJpaRepository.findVisibleRegularTasks(
            WORKSPACE_ID, startOfDay(2099, 1, 1), endOfDay(2099, 1, 1), TaskStatus.COMPLETED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findVisibleRegularTasksDoesNotExposeCompletedTaskWithoutStatusHistory() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, "이력 없는 완료 업무", TaskStatus.COMPLETED, LocalDate.of(2026, 8, 5));

    List<TaskJpaEntity> result =
        taskJpaRepository.findVisibleRegularTasks(
            WORKSPACE_ID, startOfDay(2026, 8, 5), endOfDay(2026, 8, 5), TaskStatus.COMPLETED);

    assertThat(result).isEmpty();
  }

  @Test
  void findVisibleRecurringTasksExposesCompletedRecurringTaskOnItsScheduledDate() {
    insertWorkspace(WORKSPACE_ID);
    insertRecurringTemplate(100L, WORKSPACE_ID);
    insertRecurringTask(1L, WORKSPACE_ID, 100L, TaskStatus.COMPLETED, at(2026, 8, 5));

    List<TaskJpaEntity> result =
        taskJpaRepository.findVisibleRecurringTasks(
            WORKSPACE_ID, startOfDay(2026, 8, 5), endOfDay(2026, 8, 5));

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at) "
            + "values (?, 1, ?, 10, ?, ?)",
        workspaceId,
        "ws-" + workspaceId,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertTask(
      long taskId, long workspaceId, String title, TaskStatus status, LocalDate dueAt) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, ?, ?, ?, ?, 10, ?, ?)",
        taskId,
        workspaceId,
        title,
        status.name(),
        dueAt,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertStatusHistory(
      long taskId, TaskStatus previous, TaskStatus current, LocalDateTime createdAt) {
    jdbcTemplate.update(
        "insert into task_status_history (task_id, previous_status, current_status, created_at) "
            + "values (?, ?, ?, ?)",
        taskId,
        previous.name(),
        current.name(),
        createdAt);
  }

  private void insertRecurringTemplate(long templateId, long workspaceId) {
    jdbcTemplate.update(
        "insert into recurring_task_template "
            + "(recurring_template_id, workspace_id, title, recurrence_type, recurrence_rule, is_active, created_by, created_at, updated_at) "
            + "values (?, ?, 'daily', 'DAILY', '{}', true, 10, ?, ?)",
        templateId,
        workspaceId,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRecurringTask(
      long taskId, long workspaceId, long templateId, TaskStatus status, LocalDateTime scheduledFor) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, ?, ?, '반복 업무', ?, ?, 10, ?, ?)",
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

  private LocalDateTime startOfDay(int year, int month, int day) {
    return LocalDate.of(year, month, day).atStartOfDay();
  }

  private LocalDateTime endOfDay(int year, int month, int day) {
    return LocalDate.of(year, month, day).plusDays(1).atStartOfDay();
  }
}
