package com.academy.mudogroupware.workspace.application.port;

import com.academy.mudogroupware.workspace.application.query.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.WorkspaceTaskCandidate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkspaceDetailQueryPort {

  Optional<String> findActiveWorkspaceName(Long workspaceId);

  List<Long> findMemberIds(Long workspaceId);

  List<WorkspaceTaskCandidate> findVisibleTasks(Long workspaceId, LocalDate date);

  List<TaskCommentSummary> findCommentSummaries(List<Long> taskIds);
}
