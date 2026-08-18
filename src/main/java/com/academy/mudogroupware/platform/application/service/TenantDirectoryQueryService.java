package com.academy.mudogroupware.platform.application.service;

import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantDirectoryQueryService {
  private final PlatformTenantDirectory tenantDirectory;

  public TenantDirectoryEntry resolve(String academyCode) {
    return tenantDirectory.get(academyCode);
  }
}
