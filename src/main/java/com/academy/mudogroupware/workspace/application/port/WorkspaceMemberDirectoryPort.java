package com.academy.mudogroupware.workspace.application.port;

import java.util.Set;

public interface WorkspaceMemberDirectoryPort {

  Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds);
}
