package com.academy.mudogroupware.global.presentation.security.handler;

import com.academy.mudogroupware.global.domain.common.exception.CommonErrorCode;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiErrorResponse;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper mapper;

  public void handle(HttpServletRequest q, HttpServletResponse s, AccessDeniedException e)
      throws IOException {
    s.setStatus(403);
    s.setContentType(MediaType.APPLICATION_JSON_VALUE);
    s.setCharacterEncoding(StandardCharsets.UTF_8.name());
    mapper.writeValue(
        s.getWriter(),
        GlobalApiErrorResponse.of(CommonErrorCode.ACCESS_DENIED, MDC.get("traceId")));
  }
}
