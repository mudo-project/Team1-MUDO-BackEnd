package com.academy.mudogroupware.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformTenantDirectoryTest {

  @Test
  void findAllCachesParsedResultAcrossCalls() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setTenantDirectoryJson(
        "[{\"code\":\"academy-a\",\"apiHost\":\"academy-a.ieum.store\"}]");
    PlatformTenantDirectory directory = new PlatformTenantDirectory(properties, new ObjectMapper());

    List<TenantDirectoryEntry> first = directory.findAll();
    List<TenantDirectoryEntry> second = directory.findAll();

    assertThat(first).isSameAs(second);
  }

  @Test
  void getReturnsEntryMatchingCode() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setTenantDirectoryJson(
        "[{\"code\":\"academy-d\",\"apiHost\":\"sidea-test.ieum.store\"}]");
    PlatformTenantDirectory directory = new PlatformTenantDirectory(properties, new ObjectMapper());

    TenantDirectoryEntry entry = directory.get("academy-d");

    assertThat(entry.code()).isEqualTo("academy-d");
    assertThat(entry.apiHost()).isEqualTo("sidea-test.ieum.store");
  }

  @Test
  void getThrowsWhenCodeNotFound() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setTenantDirectoryJson("[]");
    PlatformTenantDirectory directory = new PlatformTenantDirectory(properties, new ObjectMapper());

    assertThatThrownBy(() -> directory.get("academy-unknown"))
        .isInstanceOf(PlatformException.class);
  }
}
