package com.academy.mudogroupware.global.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  @Test
  void addsTenantMetadataAndTraceIdForRequestLifetime() throws Exception {
    InstanceMetadataProperties properties = new InstanceMetadataProperties();
    properties.setTenantId("academy-a");
    properties.setPlan("standard");
    properties.setDeploymentSha("abc123");
    TraceIdFilter filter = new TraceIdFilter(properties);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/test"),
        response,
        (request, servletResponse) -> {
          assertThat(MDC.get("tenantId")).isEqualTo("academy-a");
          assertThat(MDC.get("plan")).isEqualTo("standard");
          assertThat(MDC.get("deploymentSha")).isEqualTo("abc123");
          assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNotBlank();
        });

    assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
    assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
  }
}
