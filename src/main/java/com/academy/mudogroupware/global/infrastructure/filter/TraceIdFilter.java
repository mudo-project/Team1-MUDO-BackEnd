package com.academy.mudogroupware.global.infrastructure.filter;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceIdFilter extends OncePerRequestFilter {
  public static final String TRACE_ID = "traceId", TRACE_ID_HEADER = "X-Trace-Id";
  private final InstanceMetadataProperties instanceMetadata;

  public TraceIdFilter(InstanceMetadataProperties instanceMetadata) {
    this.instanceMetadata = instanceMetadata;
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    String t = UUID.randomUUID().toString().substring(0, 8);
    Map<String, String> previousContext = MDC.getCopyOfContextMap();
    try {
      MDC.put(TRACE_ID, t);
      MDC.put("tenantId", instanceMetadata.getTenantId());
      MDC.put("plan", instanceMetadata.getPlan());
      MDC.put("deploymentSha", instanceMetadata.getDeploymentSha());
      MDC.put("method", q.getMethod());
      MDC.put("requestURI", q.getRequestURI());
      MDC.put("remoteAddr", q.getRemoteAddr());
      s.setHeader(TRACE_ID_HEADER, t);
      c.doFilter(q, s);
    } finally {
      MDC.clear();
      if (previousContext != null) {
        MDC.setContextMap(previousContext);
      }
    }
  }
}
