package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.usecase.comment.TaskCommentListQueryUseCase;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskCommentListQueryService implements TaskCommentListQueryUseCase {

  private static final String UNKNOWN_NAME = "알 수 없음";

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final WorkspaceUserInfoPort workspaceUserInfoPort;

  @Override
  public PageResult<TaskCommentListItem> getComments(
      Long workspaceId, Long taskId, Long requesterId, int page, int size, boolean canReadAll) {
    Workspace workspace =
        workspaceRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);

    if (!workspace.getMemberIds().contains(requesterId) && !canReadAll) {
      throw new WorkspaceAccessDeniedException();
    }

    taskRepository.findById(workspaceId, taskId).orElseThrow(TaskNotFoundException::new);

    PageResult<TaskComment> comments = taskCommentRepository.findAllByTaskId(taskId, page, size);

    Set<Long> authorIds =
        comments.content().stream().map(TaskComment::getAuthorId).collect(Collectors.toSet());
    Map<Long, String> nameByUserId =
        workspaceUserInfoPort.findUserInfo(authorIds).stream()
            .collect(Collectors.toMap(WorkspaceMemberInfo::userId, WorkspaceMemberInfo::name));

    return comments.map(comment -> toItem(comment, nameByUserId));
  }

  private TaskCommentListItem toItem(TaskComment comment, Map<Long, String> nameByUserId) {
    String authorName = nameByUserId.getOrDefault(comment.getAuthorId(), UNKNOWN_NAME);
    return new TaskCommentListItem(
        comment.getId(),
        comment.getContent(),
        new WorkspaceMemberInfo(comment.getAuthorId(), authorName),
        comment.isCompleted(),
        comment.getCreatedAt());
  }
}
