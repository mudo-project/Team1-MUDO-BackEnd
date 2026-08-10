package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.task.TaskDetail;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
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
class TaskDetailQueryServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;
  private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 29, 9, 30);

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskStatusHistoryRepository taskStatusHistoryRepository;
  @Mock private WorkspaceUserInfoPort workspaceUserInfoPort;

  private TaskDetailQueryService service() {
    return new TaskDetailQueryService(
        workspaceRepository, taskRepository, taskStatusHistoryRepository, workspaceUserInfoPort);
  }

  @Test
  void returnsDetailWithResolvedCreatorAndLastChangedAt() {
    givenWorkspaceWithMember();
    givenTask();
    when(workspaceUserInfoPort.findUserInfo(Set.of(MEMBER_ID)))
        .thenReturn(List.of(new WorkspaceMemberInfo(MEMBER_ID, "윤예진")));
    when(taskStatusHistoryRepository.findLatestChangedAt(TASK_ID))
        .thenReturn(Optional.of(LocalDateTime.of(2026, 8, 2, 9, 0)));

    TaskDetail detail = service().getTaskDetail(WORKSPACE_ID, TASK_ID, MEMBER_ID);

    assertThat(detail.taskId()).isEqualTo(TASK_ID);
    assertThat(detail.creator()).isEqualTo(new WorkspaceMemberInfo(MEMBER_ID, "윤예진"));
    assertThat(detail.createdAt()).isEqualTo(CREATED_AT);
    assertThat(detail.lastStatusChangedAt()).isEqualTo(LocalDateTime.of(2026, 8, 2, 9, 0));
  }

  @Test
  void returnsNullLastStatusChangedAtWhenNoHistory() {
    givenWorkspaceWithMember();
    givenTask();
    when(workspaceUserInfoPort.findUserInfo(Set.of(MEMBER_ID)))
        .thenReturn(List.of(new WorkspaceMemberInfo(MEMBER_ID, "윤예진")));
    when(taskStatusHistoryRepository.findLatestChangedAt(TASK_ID)).thenReturn(Optional.empty());

    TaskDetail detail = service().getTaskDetail(WORKSPACE_ID, TASK_ID, MEMBER_ID);

    assertThat(detail.lastStatusChangedAt()).isNull();
  }

  @Test
  void fallsBackToUnknownNameWhenCreatorNotResolved() {
    givenWorkspaceWithMember();
    givenTask();
    when(workspaceUserInfoPort.findUserInfo(Set.of(MEMBER_ID))).thenReturn(List.of());
    when(taskStatusHistoryRepository.findLatestChangedAt(TASK_ID)).thenReturn(Optional.empty());

    TaskDetail detail = service().getTaskDetail(WORKSPACE_ID, TASK_ID, MEMBER_ID);

    assertThat(detail.creator()).isEqualTo(new WorkspaceMemberInfo(MEMBER_ID, "알 수 없음"));
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getTaskDetail(WORKSPACE_ID, TASK_ID, MEMBER_ID))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(() -> service().getTaskDetail(WORKSPACE_ID, TASK_ID, OUTSIDER_ID))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMember();
    when(taskRepository.findById(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getTaskDetail(WORKSPACE_ID, TASK_ID, MEMBER_ID))
        .isInstanceOf(TaskNotFoundException.class);
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace =
        Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenTask() {
    Task task =
        Task.restore(
            TASK_ID, WORKSPACE_ID, null, "성적 데이터 7월분 엑셀 정리", TaskStatus.IN_PROGRESS,
            LocalDate.of(2026, 8, 5), null, MEMBER_ID, CREATED_AT);
    when(taskRepository.findById(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }
}
