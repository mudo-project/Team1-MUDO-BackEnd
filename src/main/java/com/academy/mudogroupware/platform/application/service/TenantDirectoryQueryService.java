package com.academy.mudogroupware.platform.application.service;

import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TenantDirectoryQueryService {
  private final PlatformTenantDirectory tenantDirectory;

  public TenantDirectoryEntry resolve(String academyCode) {
    return tenantDirectory.get(academyCode);
  }
}
