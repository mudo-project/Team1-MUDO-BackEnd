package com.academy.mudogroupware.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PlatformTenantRegistryTest {

  @Test
  void findAllCachesParsedResultAcrossCalls() {
    PlatformDashboardProperties properties = new PlatformDashboardProperties();
    properties.setTenantRegistryJson(
        "[{\"code\":\"academy-a\",\"ecsCluster\":\"c\",\"ecsService\":\"s\","
            + "\"rdsIdentifier\":\"r\",\"rdsMaxConnections\":10,\"rdsAppConnectionRatio\":0.5,"
            + "\"staffBucket\":\"b1\",\"financeBucket\":\"b2\",\"s3Prefix\":\"p\"}]");
    PlatformTenantRegistry registry = new PlatformTenantRegistry(properties, new ObjectMapper());

    java.util.List<AcademyRuntime> first = registry.findAll();
    java.util.List<AcademyRuntime> second = registry.findAll();

    assertThat(first).isSameAs(second);
  }
}
