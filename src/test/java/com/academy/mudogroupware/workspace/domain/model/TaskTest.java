package com.academy.mudogroupware.workspace.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.workspace.domain.exception.IllegalTaskDueAtException;
import com.academy.mudogroupware.workspace.domain.exception.InvalidTaskStatusTransitionException;
import com.academy.mudogroupware.workspace.domain.exception.TaskDueAtRequiredException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TaskTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);
  private static final LocalDate TOMORROW = TODAY.plusDays(1);
  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;

  // --- create: 초기 상태 결정 ---

  @Test
  void createWithPastDueAtStartsDelayed() {
    Task task = Task.create(WORKSPACE_ID, "지난 업무", YESTERDAY, CREATOR_ID, TODAY);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.DELAYED);
  }

  @Test
  void createWithTodayDueAtStartsWaiting() {
    Task task = Task.create(WORKSPACE_ID, "오늘 마감", TODAY, CREATOR_ID, TODAY);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.WAITING);
  }

  @Test
  void createWithFutureDueAtStartsWaiting() {
    Task task = Task.create(WORKSPACE_ID, "미래 마감", TOMORROW, CREATOR_ID, TODAY);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.WAITING);
  }

  @Test
  void createTrimsTitleAndLeavesIdentityFieldsUnset() {
    Task task = Task.create(WORKSPACE_ID, "  제목  ", TOMORROW, CREATOR_ID, TODAY);

    assertThat(task.getTitle()).isEqualTo("제목");
    assertThat(task.getId()).isNull();
    assertThat(task.getRecurringTemplateId()).isNull();
    assertThat(task.getScheduledFor()).isNull();
    assertThat(task.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(task.getCreatedBy()).isEqualTo(CREATOR_ID);
    assertThat(task.isRecurring()).isFalse();
  }

  // --- 규칙 1: COMPLETED -> DELAYED 금지 ---

  @Test
  void completedTaskCannotBecomeDelayed() {
    Task completed = regular(TaskStatus.COMPLETED, TOMORROW);

    assertThatThrownBy(() -> completed.changeStatus(TaskStatus.DELAYED, null, TODAY))
        .isInstanceOf(InvalidTaskStatusTransitionException.class);
  }

  @Test
  void incompleteTaskCanBecomeDelayedByUser() {
    assertThat(regular(TaskStatus.WAITING, TOMORROW).changeStatus(TaskStatus.DELAYED, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.DELAYED);
    assertThat(regular(TaskStatus.IN_PROGRESS, TOMORROW).changeStatus(TaskStatus.DELAYED, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.DELAYED);
  }

  @Test
  void delayedTaskCanBecomeCompletedWithoutNewDueAt() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    assertThat(delayed.changeStatus(TaskStatus.COMPLETED, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.COMPLETED);
  }

  // --- 규칙 2: 과거 마감일 + 미완료 전환 ---

  @Test
  void reopeningPastDueTaskWithoutNewDueAtIsRejected() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    assertThatThrownBy(() -> delayed.changeStatus(TaskStatus.IN_PROGRESS, null, TODAY))
        .isInstanceOf(TaskDueAtRequiredException.class);
  }

  @Test
  void reopeningPastDueTaskWithPastNewDueAtIsRejected() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    assertThatThrownBy(() -> delayed.changeStatus(TaskStatus.IN_PROGRESS, YESTERDAY, TODAY))
        .isInstanceOf(TaskDueAtRequiredException.class);
  }

  @Test
  void reopeningPastDueTaskWithTodayAsNewDueAtIsAllowed() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    Task reopened = delayed.changeStatus(TaskStatus.IN_PROGRESS, TODAY, TODAY);

    assertThat(reopened.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(reopened.getDueAt()).isEqualTo(TODAY);
  }

  @Test
  void reopeningPastDueTaskWithFutureNewDueAtIsAllowed() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    Task reopened = delayed.changeStatus(TaskStatus.WAITING, TOMORROW, TODAY);

    assertThat(reopened.getStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(reopened.getDueAt()).isEqualTo(TOMORROW);
  }

  @Test
  void reopeningCompletedTaskWithPastDueAtRequiresNewDueAt() {
    Task completed = regular(TaskStatus.COMPLETED, YESTERDAY);

    assertThatThrownBy(() -> completed.changeStatus(TaskStatus.WAITING, null, TODAY))
        .isInstanceOf(TaskDueAtRequiredException.class);
  }

  @Test
  void reopeningCompletedTaskWithFutureDueAtNeedsNoNewDueAt() {
    Task completed = regular(TaskStatus.COMPLETED, TOMORROW);

    assertThat(completed.changeStatus(TaskStatus.WAITING, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.WAITING);
  }

  @Test
  void delayedTaskWithFutureDueAtNeedsNoNewDueAtToReopen() {
    // 사용자가 직접 미뤄둔 업무: 마감일이 아직 미래다
    Task delayed = regular(TaskStatus.DELAYED, TOMORROW);

    assertThat(delayed.changeStatus(TaskStatus.IN_PROGRESS, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void newDueAtIsAppliedEvenWhenRuleTwoDoesNotRequireIt() {
    Task waiting = regular(TaskStatus.WAITING, TOMORROW);

    Task updated = waiting.changeStatus(TaskStatus.COMPLETED, TOMORROW.plusDays(3), TODAY);

    assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(updated.getDueAt()).isEqualTo(TOMORROW.plusDays(3));
  }

  // --- 같은 상태 전이 ---

  @Test
  void sameStatusTransitionReturnsTaskWithUnchangedStatus() {
    Task waiting = regular(TaskStatus.WAITING, YESTERDAY);

    // 과거 마감일이지만 실제 전환이 아니므로 규칙 2를 적용하지 않는다
    assertThatCode(() -> waiting.changeStatus(TaskStatus.WAITING, null, TODAY)).doesNotThrowAnyException();
    assertThat(waiting.changeStatus(TaskStatus.WAITING, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.WAITING);
  }

  @Test
  void sameStatusTransitionStillAppliesNewDueAt() {
    Task waiting = regular(TaskStatus.WAITING, TOMORROW);

    Task updated = waiting.changeStatus(TaskStatus.WAITING, TOMORROW.plusDays(2), TODAY);

    assertThat(updated.getDueAt()).isEqualTo(TOMORROW.plusDays(2));
  }

  // --- 마감일 단독 수정 ---

  @Test
  void changeDueAtKeepsStatus() {
    Task delayed = regular(TaskStatus.DELAYED, YESTERDAY);

    Task updated = delayed.changeDueAt(TOMORROW);

    assertThat(updated.getStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(updated.getDueAt()).isEqualTo(TOMORROW);
  }

  // --- 마감일 없는 일반 업무 (규칙 2 면제) ---

  @Test
  void nonRecurringTaskWithNullDueAtReopensWithoutNewDueAt() {
    Task noDueAt = Task.restore(1L, WORKSPACE_ID, null, "마감일 없는 업무", TaskStatus.DELAYED, null, null, CREATOR_ID);

    Task waiting = noDueAt.changeStatus(TaskStatus.WAITING, null, TODAY);
    assertThat(waiting.getStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(waiting.getDueAt()).isNull();

    Task inProgress = noDueAt.changeStatus(TaskStatus.IN_PROGRESS, null, TODAY);
    assertThat(inProgress.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(inProgress.getDueAt()).isNull();
  }

  // --- 반복 업무 ---

  @Test
  void recurringTaskIsExemptFromRuleTwo() {
    Task recurring = recurring(TaskStatus.DELAYED);

    assertThat(recurring.changeStatus(TaskStatus.IN_PROGRESS, null, TODAY).getStatus())
        .isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void recurringTaskRejectsDueAtChange() {
    Task recurring = recurring(TaskStatus.WAITING);

    assertThatThrownBy(() -> recurring.changeDueAt(TOMORROW))
        .isInstanceOf(IllegalTaskDueAtException.class);
    assertThatThrownBy(() -> recurring.changeStatus(TaskStatus.IN_PROGRESS, TOMORROW, TODAY))
        .isInstanceOf(IllegalTaskDueAtException.class);
  }

  @Test
  void recurringTaskStillRejectsCompletedToDelayed() {
    Task recurring = Task.restore(1L, WORKSPACE_ID, 100L, "반복", TaskStatus.COMPLETED, null,
        YESTERDAY.atTime(9, 0), CREATOR_ID);

    assertThatThrownBy(() -> recurring.changeStatus(TaskStatus.DELAYED, null, TODAY))
        .isInstanceOf(InvalidTaskStatusTransitionException.class);
  }

  private Task regular(TaskStatus status, LocalDate dueAt) {
    return Task.restore(1L, WORKSPACE_ID, null, "일반 업무", status, dueAt, null, CREATOR_ID);
  }

  private Task recurring(TaskStatus status) {
    LocalDateTime scheduledFor = YESTERDAY.atTime(9, 0);
    return Task.restore(1L, WORKSPACE_ID, 100L, "반복 업무", status, null, scheduledFor, CREATOR_ID);
  }
}
