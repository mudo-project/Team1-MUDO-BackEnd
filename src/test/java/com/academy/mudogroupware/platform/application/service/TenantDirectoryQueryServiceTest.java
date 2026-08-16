package com.academy.mudogroupware.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantDirectory;
import org.junit.jupiter.api.Test;

class TenantDirectoryQueryServiceTest {

  @Test
  void resolveReturnsEntryFromDirectory() {
    PlatformTenantDirectory directory = mock(PlatformTenantDirectory.class);
    when(directory.get("academy-d")).thenReturn(new TenantDirectoryEntry("academy-d", "sidea-test.ieum.store"));
    TenantDirectoryQueryService service = new TenantDirectoryQueryService(directory);

    TenantDirectoryEntry result = service.resolve("academy-d");

    assertThat(result.code()).isEqualTo("academy-d");
    assertThat(result.apiHost()).isEqualTo("sidea-test.ieum.store");
  }
}
