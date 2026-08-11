package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.port.WorkspaceDetailQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceDetail;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskItem;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceDetailQueryServiceTest {

  private static final Long USER_ID = 10L;
  private static final Long WORKSPACE_ID = 100L;
  private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

  @Mock private WorkspaceDetailQueryPort workspaceDetailQueryPort;
  @Mock private WorkspaceListQueryPort workspaceListQueryPort;
  @Mock private WorkspaceUserInfoPort workspaceUserInfoPort;

  private WorkspaceDetailQueryService service() {
    return new WorkspaceDetailQueryService(
        workspaceDetailQueryPort, workspaceListQueryPort, workspaceUserInfoPort);
  }

  @Test
  void throwsNotFoundWhenWorkspaceDoesNotExistRegardlessOfAcademy() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }

  @Test
  void throwsAccessDeniedWhenWorkspaceExistsButRequesterCannotAccessIt() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID))
        .thenReturn(Optional.of("ws"));
    when(workspaceListQueryPort.existsAccessible(WORKSPACE_ID, USER_ID, false))
        .thenReturn(false);

    assertThatThrownBy(
            () -> service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void assemblesMembersAndTasksWithResolvedNamesAndCommentCounts() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID))
        .thenReturn(Optional.of("1월 학사 운영"));
    when(workspaceListQueryPort.existsAccessible(WORKSPACE_ID, USER_ID, false))
        .thenReturn(true);
    when(workspaceDetailQueryPort.findMemberIds(WORKSPACE_ID)).thenReturn(List.of(12L));
    WorkspaceTaskCandidate withComments =
        new WorkspaceTaskCandidate(
            101L, "청구서 발송", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 7), 25L,
            LocalDateTime.of(2026, 8, 1, 0, 0));
    WorkspaceTaskCandidate withoutComments =
        new WorkspaceTaskCandidate(
            102L, "청소 당번", TaskStatus.WAITING, null, 25L, LocalDateTime.of(2026, 8, 2, 0, 0));
    when(workspaceDetailQueryPort.findVisibleTasks(WORKSPACE_ID, DATE))
        .thenReturn(List.of(withComments, withoutComments));
    when(workspaceUserInfoPort.findUserInfo(Set.of(12L, 25L)))
        .thenReturn(
            List.of(new WorkspaceMemberInfo(12L, "김지수"), new WorkspaceMemberInfo(25L, "정다은")));
    when(workspaceDetailQueryPort.findCommentSummaries(List.of(101L, 102L)))
        .thenReturn(List.of(new TaskCommentSummary(101L, 1L, 2L)));

    WorkspaceDetail result =
        service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false);

    assertThat(result.workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(result.name()).isEqualTo("1월 학사 운영");
    assertThat(result.memberCount()).isEqualTo(1);
    assertThat(result.members()).containsExactly(new WorkspaceMemberInfo(12L, "김지수"));
    assertThat(result.taskCount()).isEqualTo(2);
    assertThat(result.tasks())
        .extracting(WorkspaceTaskItem::taskId, WorkspaceTaskItem::completedCommentCount, WorkspaceTaskItem::commentCount)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(102L, null, null),
            org.assertj.core.groups.Tuple.tuple(101L, 1L, 2L));
    assertThat(result.tasks().get(0).creator()).isEqualTo(new WorkspaceMemberInfo(25L, "정다은"));
  }

  @Test
  void sortsTasksByStatusThenDueDateAscendingWithNullsLastThenCreatedAtAscending() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID))
        .thenReturn(Optional.of("ws"));
    when(workspaceListQueryPort.existsAccessible(WORKSPACE_ID, USER_ID, false))
        .thenReturn(true);
    when(workspaceDetailQueryPort.findMemberIds(WORKSPACE_ID)).thenReturn(List.of());
    WorkspaceTaskCandidate noDueDate =
        new WorkspaceTaskCandidate(
            3L, "무기한", TaskStatus.WAITING, null, 25L, LocalDateTime.of(2026, 8, 1, 0, 0));
    WorkspaceTaskCandidate laterDue =
        new WorkspaceTaskCandidate(
            2L, "늦은 기한", TaskStatus.WAITING, LocalDate.of(2026, 8, 10), 25L,
            LocalDateTime.of(2026, 8, 1, 0, 0));
    WorkspaceTaskCandidate earlierDue =
        new WorkspaceTaskCandidate(
            1L, "빠른 기한", TaskStatus.WAITING, LocalDate.of(2026, 8, 6), 25L,
            LocalDateTime.of(2026, 8, 1, 0, 0));
    when(workspaceDetailQueryPort.findVisibleTasks(WORKSPACE_ID, DATE))
        .thenReturn(List.of(noDueDate, laterDue, earlierDue));
    when(workspaceUserInfoPort.findUserInfo(Set.of(25L)))
        .thenReturn(List.of(new WorkspaceMemberInfo(25L, "정다은")));
    when(workspaceDetailQueryPort.findCommentSummaries(List.of(3L, 2L, 1L))).thenReturn(List.of());

    WorkspaceDetail result =
        service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false);

    assertThat(result.tasks()).extracting(WorkspaceTaskItem::taskId).containsExactly(1L, 2L, 3L);
  }

  @Test
  void sortsTasksByTaskIdWhenStatusDueDateAndCreatedAtAreAllTied() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID))
        .thenReturn(Optional.of("ws"));
    when(workspaceListQueryPort.existsAccessible(WORKSPACE_ID, USER_ID, false))
        .thenReturn(true);
    when(workspaceDetailQueryPort.findMemberIds(WORKSPACE_ID)).thenReturn(List.of());
    LocalDate sameDue = LocalDate.of(2026, 8, 10);
    LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 8, 1, 0, 0);
    WorkspaceTaskCandidate second =
        new WorkspaceTaskCandidate(102L, "둘째", TaskStatus.WAITING, sameDue, 25L, sameCreatedAt);
    WorkspaceTaskCandidate first =
        new WorkspaceTaskCandidate(101L, "첫째", TaskStatus.WAITING, sameDue, 25L, sameCreatedAt);
    when(workspaceDetailQueryPort.findVisibleTasks(WORKSPACE_ID, DATE))
        .thenReturn(List.of(second, first));
    when(workspaceUserInfoPort.findUserInfo(Set.of(25L)))
        .thenReturn(List.of(new WorkspaceMemberInfo(25L, "정다은")));
    when(workspaceDetailQueryPort.findCommentSummaries(List.of(102L, 101L))).thenReturn(List.of());

    WorkspaceDetail result =
        service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false);

    assertThat(result.tasks())
        .extracting(WorkspaceTaskItem::taskId)
        .containsExactly(101L, 102L);
  }

  @Test
  void fallsBackToUnknownNameWhenMemberOrCreatorNameCannotBeResolved() {
    when(workspaceDetailQueryPort.findActiveWorkspaceName(WORKSPACE_ID))
        .thenReturn(Optional.of("ws"));
    when(workspaceListQueryPort.existsAccessible(WORKSPACE_ID, USER_ID, false))
        .thenReturn(true);
    when(workspaceDetailQueryPort.findMemberIds(WORKSPACE_ID)).thenReturn(List.of(12L));
    WorkspaceTaskCandidate candidate =
        new WorkspaceTaskCandidate(
            101L, "청구서 발송", TaskStatus.WAITING, null, 99L, LocalDateTime.of(2026, 8, 1, 0, 0));
    when(workspaceDetailQueryPort.findVisibleTasks(WORKSPACE_ID, DATE))
        .thenReturn(List.of(candidate));
    when(workspaceUserInfoPort.findUserInfo(Set.of(12L, 99L))).thenReturn(List.of());
    when(workspaceDetailQueryPort.findCommentSummaries(List.of(101L))).thenReturn(List.of());

    WorkspaceDetail result =
        service().getWorkspaceDetail(USER_ID, WORKSPACE_ID, DATE, false);

    assertThat(result.members()).containsExactly(new WorkspaceMemberInfo(12L, "알 수 없음"));
    assertThat(result.tasks().get(0).creator()).isEqualTo(new WorkspaceMemberInfo(99L, "알 수 없음"));
  }
}
