package com.academy.mudogroupware.platform.infrastructure;

import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 프론트가 로그인 전 학원 코드→API 호스트를 조회하는 공개 라우팅 정보라 dashboard host
// 여부와 무관하게 모든 활성 Task에서 항상 조회 가능해야 한다.
@Component
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
