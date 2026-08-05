package com.academy.mudogroupware.workspace.application.port;

import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import java.util.List;

public interface WorkspaceListQueryPort {

  List<WorkspaceListItem> findMine(Long academyId, Long userId);

  List<WorkspaceListItem> findAll(Long academyId, Long userId);

  boolean existsAccessible(Long workspaceId, Long academyId, Long userId, boolean canReadAll);
}
