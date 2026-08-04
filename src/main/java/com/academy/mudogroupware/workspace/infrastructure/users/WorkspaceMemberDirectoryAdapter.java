package com.academy.mudogroupware.workspace.infrastructure.users;

import com.academy.mudogroupware.users.application.usecase.UserDirectoryUseCase;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberDirectoryAdapter implements WorkspaceMemberDirectoryPort {

  private final UserDirectoryUseCase userDirectoryUseCase;

  /**
   * Consumer: workspace
   *
   * <p>Purpose: 워크스페이스 초기 참여자 검증
   */
  @Override
  public Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds) {
    return userDirectoryUseCase.findActiveUserIds(academyId, userIds);
  }
}
