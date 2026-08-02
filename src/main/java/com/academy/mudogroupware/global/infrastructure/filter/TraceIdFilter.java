package com.academy.mudogroupware.global.infrastructure.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
  public static final String TRACE_ID = "traceId", TRACE_ID_HEADER = "X-Trace-Id";

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse s, FilterChain c)
      throws ServletException, IOException {
    String t = UUID.randomUUID().toString().substring(0, 8);
    try {
      MDC.put(TRACE_ID, t);
      MDC.put("method", q.getMethod());
      MDC.put("requestURI", q.getRequestURI());
      MDC.put("remoteAddr", q.getRemoteAddr());
      s.setHeader(TRACE_ID_HEADER, t);
      c.doFilter(q, s);
    } finally {
      MDC.clear();
    }
  }
}
