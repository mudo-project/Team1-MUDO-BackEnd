package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
  TimeConfig.class,
  TaskPersistenceAdapter.class,
  TaskStatusHistoryPersistenceAdapter.class,
  RecurringTaskSkipPersistenceAdapter.class,
  TaskPersistenceMapperImpl.class
})
class TaskPersistenceAdapterDataJpaTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TaskRepository taskRepository;
  @Autowired private TaskStatusHistoryRepository taskStatusHistoryRepository;
  @Autowired private RecurringTaskSkipRepository recurringTaskSkipRepository;

  @Test
  void savesNewRegularTaskAndReturnsAssignedId() {
    insertWorkspace(WORKSPACE_ID);

    Task saved = taskRepository.save(Task.create(WORKSPACE_ID, "새 업무", TODAY, CREATOR_ID, TODAY));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("새 업무");
    assertThat(saved.getStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(saved.getDueAt()).isEqualTo(TODAY);
    assertThat(saved.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(saved.getCreatedBy()).isEqualTo(CREATOR_ID);
    assertThat(saved.isRecurring()).isFalse();
  }

  @Test
  void findByIdForUpdateRoundTripsRegularTask() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.IN_PROGRESS, TODAY);

    Optional<Task> found = taskRepository.findByIdForUpdate(WORKSPACE_ID, 1L);

    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(found.get().getDueAt()).isEqualTo(TODAY);
    assertThat(found.get().isRecurring()).isFalse();
  }

  @Test
  void findByIdForUpdateRoundTripsRecurringTask() {
    insertWorkspace(WORKSPACE_ID);
    insertRecurringTemplate(100L, WORKSPACE_ID);
    insertRecurringTask(2L, WORKSPACE_ID, 100L, TaskStatus.WAITING, TODAY.atTime(9, 0));

    Optional<Task> found = taskRepository.findByIdForUpdate(WORKSPACE_ID, 2L);

    assertThat(found).isPresent();
    assertThat(found.get().isRecurring()).isTrue();
    assertThat(found.get().getRecurringTemplateId()).isEqualTo(100L);
    assertThat(found.get().getScheduledFor()).isEqualTo(TODAY.atTime(9, 0));
    assertThat(found.get().getDueAt()).isNull();
  }

  @Test
  void findByIdForUpdateReturnsEmptyForMissingTask() {
    insertWorkspace(WORKSPACE_ID);
    assertThat(taskRepository.findByIdForUpdate(WORKSPACE_ID, 999L)).isEmpty();
  }

  @Test
  void findByIdForUpdateReturnsEmptyWhenTaskBelongsToAnotherWorkspace() {
    long otherWorkspaceId = 2L;
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(otherWorkspaceId);
    insertTask(1L, otherWorkspaceId, TaskStatus.IN_PROGRESS, TODAY);

    assertThat(taskRepository.findByIdForUpdate(WORKSPACE_ID, 1L)).isEmpty();
  }

  @Test
  void savingExistingTaskUpdatesStatusAndDueAt() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.DELAYED, TODAY.minusDays(1));
    Task loaded = taskRepository.findByIdForUpdate(WORKSPACE_ID, 1L).orElseThrow();

    taskRepository.save(loaded.changeStatus(TaskStatus.IN_PROGRESS, TODAY.plusDays(1), TODAY));

    Task reloaded = taskRepository.findByIdForUpdate(WORKSPACE_ID, 1L).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(reloaded.getDueAt()).isEqualTo(TODAY.plusDays(1));
  }

  @Test
  void deleteRemovesTaskWithCommentsMentionsAndHistories() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.IN_PROGRESS, TODAY);
    insertStatusHistory(1L, TaskStatus.WAITING, TaskStatus.IN_PROGRESS);
    insertComment(50L, 1L);
    insertMention(50L, 20L);

    taskRepository.delete(1L);

    assertThat(count("task", "task_id = 1")).isZero();
    assertThat(count("task_status_history", "task_id = 1")).isZero();
    assertThat(count("task_comment", "comment_id = 50")).isZero();
    assertThat(count("task_comment_mention", "comment_id = 50")).isZero();
  }

  @Test
  void deleteLeavesOtherTasksUntouched() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.IN_PROGRESS, TODAY);
    insertTask(2L, WORKSPACE_ID, TaskStatus.WAITING, TODAY);
    insertStatusHistory(2L, TaskStatus.WAITING, TaskStatus.IN_PROGRESS);

    taskRepository.delete(1L);

    assertThat(count("task", "task_id = 2")).isEqualTo(1);
    assertThat(count("task_status_history", "task_id = 2")).isEqualTo(1);
  }

  @Test
  void appendPersistsStatusHistoryWithGivenFields() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.WAITING, TODAY);

    taskStatusHistoryRepository.append(
        TaskStatusHistory.userChanged(1L, null, TaskStatus.WAITING, CREATOR_ID));
    taskStatusHistoryRepository.append(
        TaskStatusHistory.systemChanged(1L, TaskStatus.WAITING, TaskStatus.DELAYED));

    assertThat(count("task_status_history", "task_id = 1 and previous_status is null and changed_by = 10"))
        .isEqualTo(1);
    assertThat(count("task_status_history", "task_id = 1 and current_status = 'DELAYED' and changed_by is null"))
        .isEqualTo(1);
  }

  @Test
  void saveIfAbsentIsIdempotentForSameOccurrence() {
    insertWorkspace(WORKSPACE_ID);
    insertRecurringTemplate(100L, WORKSPACE_ID);
    LocalDateTime scheduledFor = TODAY.atTime(9, 0);

    recurringTaskSkipRepository.saveIfAbsent(100L, scheduledFor);
    recurringTaskSkipRepository.saveIfAbsent(100L, scheduledFor);

    assertThat(count("recurring_task_skip", "recurring_template_id = 100")).isEqualTo(1);
  }

  @Test
  void findOverdueRegularTasksReturnsDomainModels() {
    insertWorkspace(WORKSPACE_ID);
    insertTask(1L, WORKSPACE_ID, TaskStatus.IN_PROGRESS, TODAY.minusDays(1));
    insertTask(2L, WORKSPACE_ID, TaskStatus.IN_PROGRESS, TODAY);

    assertThat(taskRepository.findOverdueRegularTasks(TODAY))
        .extracting(Task::getId)
        .containsExactly(1L);
  }

  @Test
  void findOverdueRecurringTasksReturnsDomainModels() {
    insertWorkspace(WORKSPACE_ID);
    insertRecurringTemplate(100L, WORKSPACE_ID);
    insertRecurringTask(1L, WORKSPACE_ID, 100L, TaskStatus.WAITING, TODAY.minusDays(1).atTime(9, 0));
    insertRecurringTask(2L, WORKSPACE_ID, 100L, TaskStatus.WAITING, TODAY.atTime(9, 0));

    assertThat(taskRepository.findOverdueRecurringTasks(TODAY.atStartOfDay()))
        .extracting(Task::getId)
        .containsExactly(1L);
  }

  private long count(String table, String where) {
    return jdbcTemplate.queryForObject("select count(*) from " + table + " where " + where, Long.class);
  }

  private void insertWorkspace(long workspaceId) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at) "
            + "values (?, 1, ?, 10, ?, ?)",
        workspaceId, "ws-" + workspaceId, at(), at());
  }

  private void insertTask(long taskId, long workspaceId, TaskStatus status, LocalDate dueAt) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, ?, '일반 업무', ?, ?, 10, ?, ?)",
        taskId, workspaceId, status.name(), dueAt, at(), at());
  }

  private void insertRecurringTemplate(long templateId, long workspaceId) {
    jdbcTemplate.update(
        "insert into recurring_task_template "
            + "(recurring_template_id, workspace_id, title, recurrence_type, recurrence_rule, created_by, created_at, updated_at) "
            + "values (?, ?, 'weekly', 'WEEKLY', '{}', 10, ?, ?)",
        templateId, workspaceId, at(), at());
  }

  private void insertRecurringTask(
      long taskId, long workspaceId, long templateId, TaskStatus status, LocalDateTime scheduledFor) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, ?, ?, '반복 업무', ?, ?, 10, ?, ?)",
        taskId, workspaceId, templateId, status.name(), scheduledFor, at(), at());
  }

  private void insertStatusHistory(long taskId, TaskStatus previous, TaskStatus current) {
    jdbcTemplate.update(
        "insert into task_status_history (task_id, previous_status, current_status, created_at) "
            + "values (?, ?, ?, ?)",
        taskId, previous.name(), current.name(), at());
  }

  private void insertComment(long commentId, long taskId) {
    jdbcTemplate.update(
        "insert into task_comment (comment_id, task_id, content, is_completed, author_id, created_at, updated_at) "
            + "values (?, ?, '댓글', false, 10, ?, ?)",
        commentId, taskId, at(), at());
  }

  private void insertMention(long commentId, long mentionedUserId) {
    jdbcTemplate.update(
        "insert into task_comment_mention (comment_id, mentioned_user_id, created_at) values (?, ?, ?)",
        commentId, mentionedUserId, at());
  }

  private LocalDateTime at() {
    return LocalDateTime.of(2026, 8, 1, 9, 0);
  }
}
