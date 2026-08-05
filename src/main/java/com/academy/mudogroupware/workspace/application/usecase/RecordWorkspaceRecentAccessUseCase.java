package com.academy.mudogroupware.workspace.application.usecase;

public interface RecordWorkspaceRecentAccessUseCase {

  void recordRecentAccess(Long academyId, Long userId, Long workspaceId, boolean canReadAll);
}
