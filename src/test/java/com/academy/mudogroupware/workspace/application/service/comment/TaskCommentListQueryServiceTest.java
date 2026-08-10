package com.academy.mudogroupware.workspace.application.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
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
class TaskCommentListQueryServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;
  @Mock private WorkspaceUserInfoPort workspaceUserInfoPort;

  private TaskCommentListQueryService service() {
    return new TaskCommentListQueryService(
        workspaceRepository, taskRepository, taskCommentRepository, workspaceUserInfoPort);
  }

  @Test
  void returnsCommentsWithResolvedAuthorNames() {
    givenWorkspaceWithMember();
    givenTask();
    TaskComment comment =
        TaskComment.restore(
            1L, TASK_ID, MEMBER_ID, "수학A반 완료", true, MEMBER_ID,
            LocalDateTime.of(2026, 8, 1, 16, 0), List.of(),
            LocalDateTime.of(2026, 8, 1, 16, 0), LocalDateTime.of(2026, 8, 1, 16, 0));
    when(taskCommentRepository.findAllByTaskId(TASK_ID, 0, 20))
        .thenReturn(PageResult.of(List.of(comment), 0, 20, false));
    when(workspaceUserInfoPort.findUserInfo(Set.of(MEMBER_ID)))
        .thenReturn(List.of(new WorkspaceMemberInfo(MEMBER_ID, "윤예진")));

    PageResult<TaskCommentListItem> result =
        service().getComments(WORKSPACE_ID, TASK_ID, MEMBER_ID, 0, 20, false);

    assertThat(result.content()).hasSize(1);
    TaskCommentListItem item = result.content().get(0);
    assertThat(item.commentId()).isEqualTo(1L);
    assertThat(item.content()).isEqualTo("수학A반 완료");
    assertThat(item.author()).isEqualTo(new WorkspaceMemberInfo(MEMBER_ID, "윤예진"));
    assertThat(item.completed()).isTrue();
    assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 16, 0));
  }

  @Test
  void fallsBackToUnknownNameWhenAuthorNotResolved() {
    givenWorkspaceWithMember();
    givenTask();
    TaskComment comment =
        TaskComment.restore(
            1L, TASK_ID, MEMBER_ID, "내용", false, null, null, List.of(),
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 9, 0));
    when(taskCommentRepository.findAllByTaskId(TASK_ID, 0, 20))
        .thenReturn(PageResult.of(List.of(comment), 0, 20, false));
    when(workspaceUserInfoPort.findUserInfo(Set.of(MEMBER_ID))).thenReturn(List.of());

    PageResult<TaskCommentListItem> result =
        service().getComments(WORKSPACE_ID, TASK_ID, MEMBER_ID, 0, 20, false);

    assertThat(result.content().get(0).author()).isEqualTo(new WorkspaceMemberInfo(MEMBER_ID, "알 수 없음"));
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getComments(WORKSPACE_ID, TASK_ID, MEMBER_ID, 0, 20, false))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(() -> service().getComments(WORKSPACE_ID, TASK_ID, OUTSIDER_ID, 0, 20, false))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void allowsNonMemberWhenCanReadAllIsTrue() {
    givenWorkspaceWithMember();
    givenTask();
    when(taskCommentRepository.findAllByTaskId(TASK_ID, 0, 20))
        .thenReturn(PageResult.of(List.of(), 0, 20, false));

    PageResult<TaskCommentListItem> result =
        service().getComments(WORKSPACE_ID, TASK_ID, OUTSIDER_ID, 0, 20, true);

    assertThat(result.content()).isEmpty();
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMember();
    when(taskRepository.findById(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getComments(WORKSPACE_ID, TASK_ID, MEMBER_ID, 0, 20, false))
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
            TASK_ID, WORKSPACE_ID, null, "업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 10),
            null, MEMBER_ID);
    when(taskRepository.findById(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }
}
