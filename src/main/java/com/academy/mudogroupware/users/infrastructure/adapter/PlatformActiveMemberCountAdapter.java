package com.academy.mudogroupware.users.infrastructure.adapter;

import com.academy.mudogroupware.platform.application.port.ActiveMemberCountPort;
import com.academy.mudogroupware.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformActiveMemberCountAdapter implements ActiveMemberCountPort {
  private final UserRepository userRepository;

  /**
   * Consumer: platform dashboard.
   * Purpose: exposes the current tenant's active member count for a platform metric.
   */
  @Override
  public long countActiveMembers() {
    return userRepository.countActiveUsers();
  }
}
