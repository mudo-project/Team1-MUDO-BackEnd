package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceDetailQueryPort;
import com.academy.mudogroupware.workspace.application.query.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskCommentJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskCommentJpaRepository.TaskCommentSummaryRow;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceDetailQueryAdapter implements WorkspaceDetailQueryPort {

  private final WorkspaceJpaRepository workspaceJpaRepository;
  private final TaskJpaRepository taskJpaRepository;
  private final TaskCommentJpaRepository taskCommentJpaRepository;

  @Override
  public Optional<String> findActiveWorkspaceName(Long workspaceId) {
    return workspaceJpaRepository.findActiveWorkspaceName(workspaceId);
  }

  @Override
  public List<Long> findMemberIds(Long workspaceId) {
    return workspaceJpaRepository.findMemberUserIds(workspaceId);
  }

  @Override
  public List<WorkspaceTaskCandidate> findVisibleTasks(Long workspaceId, LocalDate date) {
    List<TaskJpaEntity> tasks = new ArrayList<>();
    tasks.addAll(
        taskJpaRepository.findVisibleRegularTasks(workspaceId, date, TaskStatus.COMPLETED));
    tasks.addAll(taskJpaRepository.findVisibleRecurringTasks(workspaceId, date));

    return tasks.stream().map(this::toCandidate).toList();
  }

  @Override
  public List<TaskCommentSummary> findCommentSummaries(List<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return List.of();
    }
    return taskCommentJpaRepository.summarizeByTaskIds(taskIds).stream()
        .map(this::toSummary)
        .toList();
  }

  private WorkspaceTaskCandidate toCandidate(TaskJpaEntity task) {
    return new WorkspaceTaskCandidate(
        task.getId(),
        task.getTitle(),
        task.getStatus(),
        task.getDueAt(),
        task.getCreatedBy(),
        task.getCreatedAt());
  }

  private TaskCommentSummary toSummary(TaskCommentSummaryRow row) {
    return new TaskCommentSummary(row.getTaskId(), row.getCompletedCount(), row.getTotalCount());
  }
}
