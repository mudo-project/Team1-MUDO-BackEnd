package com.academy.mudogroupware.workspace.application.port;

import java.time.LocalDateTime;

public interface WorkspaceRecentAccessPort {

  void upsert(Long userId, Long workspaceId, LocalDateTime accessedAt);
}
