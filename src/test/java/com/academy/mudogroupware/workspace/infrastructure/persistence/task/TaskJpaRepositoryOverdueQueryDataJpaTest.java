package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
// Replace.NONE keeps the H2 MySQL-compatibility-mode datasource configured in
// src/test/resources/application.yaml (jdbc:h2:mem:...;MODE=MySQL) instead of letting
// @DataJpaTest swap in a plain embedded H2, so that function('DATE', ...) resolves.
// This is still H2 (MySQL emulation), not a real MySQL instance -- unlike the
// Testcontainers-backed *MySqlIntegrationTest classes, which run against genuine MySQL.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TimeConfig.class)
class TaskJpaRepositoryOverdueQueryDataJpaTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TaskJpaRepository taskJpaRepository;

  @Test
  void findOverdueRegularTasksExcludesCompletedDelayedAndNotYetDueTasks() {
    insertWorkspace(1L);
    insertRegularTask(1L, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 4));
    insertRegularTask(2L, TaskStatus.COMPLETED, LocalDate.of(2026, 8, 4));
    insertRegularTask(3L, TaskStatus.DELAYED, LocalDate.of(2026, 8, 4));
    insertRegularTask(4L, TaskStatus.WAITING, LocalDate.of(2026, 8, 5));
    insertRegularTask(5L, TaskStatus.WAITING, null);

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRegularTasks(TODAY, TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findOverdueRecurringTasksExcludesCompletedDelayedAndNotYetDueOccurrences() {
    insertWorkspace(1L);
    insertRecurringTemplate(100L, 1L);
    insertRecurringTask(1L, 100L, TaskStatus.WAITING, LocalDateTime.of(2026, 8, 3, 9, 0));
    insertRecurringTask(2L, 100L, TaskStatus.COMPLETED, LocalDateTime.of(2026, 8, 3, 10, 0));
    insertRecurringTask(3L, 100L, TaskStatus.DELAYED, LocalDateTime.of(2026, 8, 3, 11, 0));
    insertRecurringTask(4L, 100L, TaskStatus.WAITING, at(2026, 8, 5));

    List<TaskJpaEntity> result =
        taskJpaRepository.findOverdueRecurringTasks(TODAY, TaskStatus.COMPLETED, TaskStatus.DELAYED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at) "
            + "values (?, 1, 'ws', 10, ?, ?)",
        workspaceId,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRegularTask(long taskId, TaskStatus status, LocalDate dueAt) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, 1, 't', ?, ?, 10, ?, ?)",
        taskId,
        status.name(),
        dueAt,
        at(2026, 8, 1),
        at(2026, 8, 1));
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

  private void insertRecurringTask(long taskId, long templateId, TaskStatus status, LocalDateTime scheduledFor) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, 1, ?, 't', ?, ?, 10, ?, ?)",
        taskId,
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
