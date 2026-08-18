package com.academy.mudogroupware.platform.infrastructure;

import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PlatformTenantDirectory {
  private final PlatformDashboardProperties properties;
  private final ObjectMapper objectMapper;

  private volatile List<TenantDirectoryEntry> cachedEntries;

  public List<TenantDirectoryEntry> findAll() {
    List<TenantDirectoryEntry> entries = cachedEntries;
    if (entries == null) {
      synchronized (this) {
        entries = cachedEntries;
        if (entries == null) {
          entries = parse();
          cachedEntries = entries;
        }
      }
    }
    return entries;
  }

  private List<TenantDirectoryEntry> parse() {
    try {
      return objectMapper.readValue(
          properties.getTenantDirectoryJson(), new TypeReference<List<TenantDirectoryEntry>>() {});
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  public TenantDirectoryEntry get(String academyCode) {
    return findAll().stream()
        .filter(entry -> entry.code().equals(academyCode))
        .findFirst()
        .orElseThrow(() -> new PlatformException(PlatformErrorCode.ACADEMY_NOT_FOUND));
  }
}
