package com.academy.mudogroupware.workspace.application.usecase.workspace;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListScope;
import java.util.List;

public interface WorkspaceQueryUseCase {

  List<WorkspaceListItem> getWorkspaces(Long academyId, Long userId, WorkspaceListScope scope);
}
