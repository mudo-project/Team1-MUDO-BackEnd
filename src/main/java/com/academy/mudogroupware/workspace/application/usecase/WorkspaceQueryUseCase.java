package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListScope;
import java.util.List;

public interface WorkspaceQueryUseCase {

  List<WorkspaceListItem> getWorkspaces(Long academyId, Long userId, WorkspaceListScope scope);
}
