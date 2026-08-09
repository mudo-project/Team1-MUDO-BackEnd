package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.task.TaskDetail;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.usecase.task.TaskDetailQueryUseCase;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskDetailQueryService implements TaskDetailQueryUseCase {

  private static final String UNKNOWN_NAME = "알 수 없음";

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskStatusHistoryRepository taskStatusHistoryRepository;
  private final WorkspaceUserInfoPort workspaceUserInfoPort;

  @Override
  public TaskDetail getTaskDetail(Long workspaceId, Long taskId, Long requesterId) {
    Workspace workspace =
        workspaceRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);

    if (!workspace.getMemberIds().contains(requesterId)) {
      throw new WorkspaceAccessDeniedException();
    }

    Task task = taskRepository.findById(workspaceId, taskId).orElseThrow(TaskNotFoundException::new);

    String creatorName =
        workspaceUserInfoPort.findUserInfo(Set.of(task.getCreatedBy())).stream()
            .filter(info -> info.userId().equals(task.getCreatedBy()))
            .map(WorkspaceMemberInfo::name)
            .findFirst()
            .orElse(UNKNOWN_NAME);

    LocalDateTime lastStatusChangedAt =
        taskStatusHistoryRepository.findLatestChangedAt(taskId).orElse(null);

    return new TaskDetail(
        task.getId(),
        task.getTitle(),
        new WorkspaceMemberInfo(task.getCreatedBy(), creatorName),
        task.getCreatedAt(),
        task.getStatus(),
        task.getDueAt(),
        lastStatusChangedAt);
  }
}
