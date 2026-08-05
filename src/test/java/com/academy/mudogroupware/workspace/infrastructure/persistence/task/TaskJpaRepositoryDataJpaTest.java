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
            WORKSPACE_ID, LocalDate.of(2099, 1, 1), TaskStatus.COMPLETED);

    assertThat(result).extracting(TaskJpaEntity::getId).containsExactly(1L);
  }

  @Test
  void findVisibleRegularTasksReturnsCompletedTaskOnlyOnItsLastCompletedDate() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, "완료 업무", TaskStatus.COMPLETED, LocalDate.of(2026, 8, 5));
    insertStatusHistory(1L, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, at(2026, 8, 5));

    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, LocalDate.of(2026, 8, 5), TaskStatus.COMPLETED))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(1L);
    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, LocalDate.of(2026, 8, 6), TaskStatus.COMPLETED))
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
                WORKSPACE_ID, LocalDate.of(2026, 8, 3), TaskStatus.COMPLETED))
        .isEmpty();
    assertThat(
            taskJpaRepository.findVisibleRegularTasks(
                WORKSPACE_ID, LocalDate.of(2026, 8, 6), TaskStatus.COMPLETED))
        .extracting(TaskJpaEntity::getId)
        .containsExactly(1L);
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at) "
            + "values (?, 1, 'ws', 10, ?, ?)",
        workspaceId,
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

  private LocalDateTime at(int year, int month, int day) {
    return LocalDateTime.of(year, month, day, 9, 0);
  }
}
