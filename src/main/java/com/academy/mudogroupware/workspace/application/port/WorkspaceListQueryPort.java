package com.academy.mudogroupware.workspace.application.port;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import java.util.List;

public interface WorkspaceListQueryPort {

  List<WorkspaceListItem> findMine(Long userId);

  List<WorkspaceListItem> findAll(Long userId);

  boolean existsAccessible(Long workspaceId, Long userId, boolean canReadAll);
}
