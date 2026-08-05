package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.port.WorkspaceDetailQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.WorkspaceDetail;
import com.academy.mudogroupware.workspace.application.query.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.query.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.application.query.WorkspaceTaskItem;
import com.academy.mudogroupware.workspace.application.usecase.WorkspaceDetailQueryUseCase;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceDetailQueryService implements WorkspaceDetailQueryUseCase {

  private static final Comparator<WorkspaceTaskCandidate> TASK_ORDER =
      Comparator.comparing(WorkspaceTaskCandidate::status)
          .thenComparing(WorkspaceTaskCandidate::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(WorkspaceTaskCandidate::createdAt);

  private final WorkspaceDetailQueryPort workspaceDetailQueryPort;
  private final WorkspaceListQueryPort workspaceListQueryPort;
  private final WorkspaceUserInfoPort workspaceUserInfoPort;

  @Override
  public WorkspaceDetail getWorkspaceDetail(
      Long academyId, Long userId, Long workspaceId, LocalDate date, boolean canReadAll) {
    String name =
        workspaceDetailQueryPort
            .findActiveWorkspaceName(workspaceId)
            .orElseThrow(WorkspaceNotFoundException::new);

    if (!workspaceListQueryPort.existsAccessible(workspaceId, academyId, userId, canReadAll)) {
      throw new WorkspaceAccessDeniedException();
    }

    List<Long> memberIds = workspaceDetailQueryPort.findMemberIds(workspaceId);
    List<WorkspaceTaskCandidate> candidates =
        workspaceDetailQueryPort.findVisibleTasks(workspaceId, date);

    Set<Long> userIdsToResolve = new LinkedHashSet<>(memberIds);
    candidates.forEach(candidate -> userIdsToResolve.add(candidate.createdBy()));
    Map<Long, String> nameByUserId =
        workspaceUserInfoPort.findUserInfo(userIdsToResolve).stream()
            .collect(Collectors.toMap(WorkspaceMemberInfo::userId, WorkspaceMemberInfo::name));

    List<WorkspaceMemberInfo> members =
        memberIds.stream().map(id -> new WorkspaceMemberInfo(id, nameByUserId.get(id))).toList();

    Map<Long, TaskCommentSummary> commentSummaryByTaskId =
        workspaceDetailQueryPort
            .findCommentSummaries(candidates.stream().map(WorkspaceTaskCandidate::taskId).toList())
            .stream()
            .collect(Collectors.toMap(TaskCommentSummary::taskId, summary -> summary));

    List<WorkspaceTaskItem> tasks =
        candidates.stream()
            .sorted(TASK_ORDER)
            .map(candidate -> toTaskItem(candidate, nameByUserId, commentSummaryByTaskId))
            .toList();

    return new WorkspaceDetail(workspaceId, name, members, tasks);
  }

  private WorkspaceTaskItem toTaskItem(
      WorkspaceTaskCandidate candidate,
      Map<Long, String> nameByUserId,
      Map<Long, TaskCommentSummary> commentSummaryByTaskId) {
    WorkspaceMemberInfo creator =
        new WorkspaceMemberInfo(candidate.createdBy(), nameByUserId.get(candidate.createdBy()));
    TaskCommentSummary summary = commentSummaryByTaskId.get(candidate.taskId());

    return new WorkspaceTaskItem(
        candidate.taskId(),
        candidate.title(),
        candidate.status(),
        creator,
        candidate.dueAt(),
        summary == null ? null : summary.completedCount(),
        summary == null ? null : summary.totalCount());
  }
}
