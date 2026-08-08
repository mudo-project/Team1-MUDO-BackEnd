package com.academy.mudogroupware.users.infrastructure.persistence;

import com.academy.mudogroupware.users.domain.repository.UserRepository;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceUserInfoAdapter implements WorkspaceUserInfoPort {

  private final UserRepository userRepository;

  /**
   * Consumer: workspace
   *
   * <p>Purpose: 워크스페이스 상세 조회 화면의 참여자·업무 생성자 표시명 조회
   */
  @Override
  public List<WorkspaceMemberInfo> findUserInfo(Set<Long> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return userRepository.findAllById(userIds).stream()
        .map(user -> new WorkspaceMemberInfo(user.getId(), user.getName()))
        .toList();
  }
}
