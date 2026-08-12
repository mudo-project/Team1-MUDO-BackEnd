package com.academy.mudogroupware.platform.infrastructure;

import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PlatformTenantRegistry {
  private final PlatformDashboardProperties properties;
  private final ObjectMapper objectMapper;

  public List<AcademyRuntime> findAll() {
    try {
      return objectMapper.readValue(properties.getTenantRegistryJson(), new TypeReference<List<AcademyRuntime>>() {}).stream()
          .sorted(Comparator.comparing(AcademyRuntime::code))
          .toList();
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  public AcademyRuntime get(String academyCode) {
    return findAll().stream()
        .filter(academy -> academy.code().equals(academyCode))
        .findFirst()
        .orElseThrow(() -> new PlatformException(PlatformErrorCode.ACADEMY_NOT_FOUND));
  }
}
