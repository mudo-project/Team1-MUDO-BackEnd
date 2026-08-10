package com.academy.mudogroupware.workspace.application.usecase.workspace;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceDetail;
import java.time.LocalDate;

public interface WorkspaceDetailQueryUseCase {

  WorkspaceDetail getWorkspaceDetail(
      Long academyId, Long userId, Long workspaceId, LocalDate date, boolean canReadAll);
}
