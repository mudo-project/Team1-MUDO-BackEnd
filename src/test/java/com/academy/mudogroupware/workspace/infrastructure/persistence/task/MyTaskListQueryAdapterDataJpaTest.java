package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.WorkspacePersistenceTestConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(WorkspacePersistenceTestConfig.class)
class MyTaskListQueryAdapterDataJpaTest {

  private static final long REQUESTER_ID = 10L;
  private static final long OTHER_USER_ID = 20L;
  private static final long WORKSPACE_1 = 1L;
  private static final long WORKSPACE_2_NOT_MEMBER = 2L;
  private static final long WORKSPACE_3 = 3L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MyTaskListQueryAdapter myTaskListQueryAdapter;

  @Test
  void findMineExcludesTasksFromWorkspacesRequesterIsNotAMemberOf() {
    insertWorkspace(WORKSPACE_1, "내 워크스페이스");
    insertWorkspace(WORKSPACE_2_NOT_MEMBER, "남의 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertMember(WORKSPACE_2_NOT_MEMBER, OTHER_USER_ID);
    insertTask(101L, WORKSPACE_1, "내 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));
    insertTask(102L, WORKSPACE_2_NOT_MEMBER, "남의 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 1));

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 0, 20);

    assertThat(result.content()).extracting(MyTaskListItem::taskId).containsExactly(101L);
  }

  @Test
  void findMineExcludesTasksFromSoftDeletedWorkspacesEvenWhenStillAMember() {
    insertWorkspace(WORKSPACE_1, "삭제될 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertTask(101L, WORKSPACE_1, "삭제된 워크스페이스 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));
    markWorkspaceDeleted(WORKSPACE_1);

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 0, 20);

    assertThat(result.content()).isEmpty();
  }

  @Test
  void findMineExcludesCompletedTasksEvenWhenStatusFilterIsNull() {
    insertWorkspace(WORKSPACE_1, "내 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertTask(101L, WORKSPACE_1, "완료 업무", TaskStatus.COMPLETED, LocalDate.of(2026, 8, 1));
    insertTask(102L, WORKSPACE_1, "대기 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 0, 20);

    assertThat(result.content()).extracting(MyTaskListItem::taskId).containsExactly(102L);
  }

  @Test
  void findMineFiltersBySingleStatusWhenProvided() {
    insertWorkspace(WORKSPACE_1, "내 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertTask(101L, WORKSPACE_1, "지연 업무", TaskStatus.DELAYED, LocalDate.of(2026, 8, 1));
    insertTask(102L, WORKSPACE_1, "대기 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, List.of(TaskStatus.DELAYED), null, 0, 20);

    assertThat(result.content()).extracting(MyTaskListItem::taskId).containsExactly(101L);
  }

  @Test
  void findMineFiltersByWorkspaceIdWhenProvided() {
    insertWorkspace(WORKSPACE_1, "워크스페이스1");
    insertWorkspace(WORKSPACE_3, "워크스페이스3");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertMember(WORKSPACE_3, REQUESTER_ID);
    insertTask(101L, WORKSPACE_1, "워크스페이스1 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));
    insertTask(102L, WORKSPACE_3, "워크스페이스3 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 20));

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), WORKSPACE_3, 0, 20);

    assertThat(result.content()).extracting(MyTaskListItem::taskId).containsExactly(102L);
    assertThat(result.content().get(0).workspaceName()).isEqualTo("워크스페이스3");
  }

  @Test
  void findMineOrdersByEffectiveDueAtAscendingUsingScheduledForWhenDueAtIsNull() {
    insertWorkspace(WORKSPACE_1, "내 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertRecurringTemplate(500L, WORKSPACE_1);
    // dueAt 오름차순 기준 기대 순서: 104(08-05, DELAYED) -> 105(08-06, 반복업무 scheduledFor) -> 101(08-15) -> 106(08-20)
    insertTask(101L, WORKSPACE_1, "일반 업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 15));
    insertTask(104L, WORKSPACE_1, "지연 업무", TaskStatus.DELAYED, LocalDate.of(2026, 8, 5));
    insertTask(106L, WORKSPACE_1, "먼 업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 20));
    insertRecurringTask(105L, WORKSPACE_1, 500L, TaskStatus.WAITING, LocalDateTime.of(2026, 8, 6, 9, 0));

    PageResult<MyTaskListItem> result =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 0, 20);

    assertThat(result.content())
        .extracting(MyTaskListItem::taskId)
        .containsExactly(104L, 105L, 101L, 106L);
    assertThat(result.content().get(1).dueAt()).isEqualTo(LocalDate.of(2026, 8, 6));
  }

  @Test
  void findMinePaginatesWithPageSizeAndHasNext() {
    insertWorkspace(WORKSPACE_1, "내 워크스페이스");
    insertMember(WORKSPACE_1, REQUESTER_ID);
    insertTask(101L, WORKSPACE_1, "업무1", TaskStatus.WAITING, LocalDate.of(2026, 8, 1));
    insertTask(102L, WORKSPACE_1, "업무2", TaskStatus.WAITING, LocalDate.of(2026, 8, 2));
    insertTask(103L, WORKSPACE_1, "업무3", TaskStatus.WAITING, LocalDate.of(2026, 8, 3));

    PageResult<MyTaskListItem> firstPage =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 0, 2);
    PageResult<MyTaskListItem> secondPage =
        myTaskListQueryAdapter.findMine(REQUESTER_ID, defaultStatuses(), null, 1, 2);

    assertThat(firstPage.content()).extracting(MyTaskListItem::taskId).containsExactly(101L, 102L);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.content()).extracting(MyTaskListItem::taskId).containsExactly(103L);
    assertThat(secondPage.hasNext()).isFalse();
  }

  private List<TaskStatus> defaultStatuses() {
    return List.of(TaskStatus.WAITING, TaskStatus.IN_PROGRESS, TaskStatus.DELAYED);
  }

  private void insertWorkspace(long workspaceId, String name) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, name, created_by, created_at, updated_at) "
            + "values (?, ?, ?, ?, ?)",
        workspaceId,
        name,
        REQUESTER_ID,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void markWorkspaceDeleted(long workspaceId) {
    jdbcTemplate.update(
        "update workspace set deleted_at = ? where workspace_id = ?", at(2026, 8, 2), workspaceId);
  }

  private void insertMember(long workspaceId, long userId) {
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId,
        userId,
        at(2026, 8, 1));
  }

  private void insertTask(
      long taskId, long workspaceId, String title, TaskStatus status, LocalDate dueAt) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, title, status, due_at, created_by, created_at, updated_at) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
        taskId,
        workspaceId,
        title,
        status.name(),
        dueAt,
        REQUESTER_ID,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRecurringTemplate(long templateId, long workspaceId) {
    jdbcTemplate.update(
        "insert into recurring_task_template "
            + "(recurring_template_id, workspace_id, title, recurrence_type, recurrence_rule, created_by, created_at, updated_at) "
            + "values (?, ?, '반복 업무', 'WEEKLY', '{}', ?, ?, ?)",
        templateId,
        workspaceId,
        REQUESTER_ID,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private void insertRecurringTask(
      long taskId, long workspaceId, long templateId, TaskStatus status, LocalDateTime scheduledFor) {
    jdbcTemplate.update(
        "insert into task (task_id, workspace_id, recurring_template_id, title, status, scheduled_for, created_by, created_at, updated_at) "
            + "values (?, ?, ?, '반복 업무 발생', ?, ?, ?, ?, ?)",
        taskId,
        workspaceId,
        templateId,
        status.name(),
        scheduledFor,
        REQUESTER_ID,
        at(2026, 8, 1),
        at(2026, 8, 1));
  }

  private LocalDateTime at(int year, int month, int day) {
    return LocalDateTime.of(year, month, day, 9, 0);
  }
}
