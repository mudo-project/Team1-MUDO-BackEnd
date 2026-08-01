package com.academy.mudogroupware.global.presentation.security.handler;

import com.academy.mudogroupware.global.domain.common.exception.*;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiErrorResponse;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper mapper;

  public void commence(HttpServletRequest q, HttpServletResponse s, AuthenticationException e)
      throws IOException {
    ErrorCode c =
        q.getAttribute("authErrorCode") instanceof ErrorCode x ? x : CommonErrorCode.UNAUTHORIZED;
    s.setStatus(c.getHttpStatus().value());
    s.setContentType(MediaType.APPLICATION_JSON_VALUE);
    s.setCharacterEncoding(StandardCharsets.UTF_8.name());
    mapper.writeValue(s.getWriter(), GlobalApiErrorResponse.of(c, MDC.get("traceId")));
  }
}
