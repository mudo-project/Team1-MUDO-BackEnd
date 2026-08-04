package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import com.academy.mudogroupware.global.infrastructure.security.websocket.JwtChannelInterceptor;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;

class JwtChannelInterceptorTest {
  @Test
  void authenticatesConnectCookieToken() {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    JwtTokenProvider provider = new JwtTokenProvider(p);
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
    headers.setSessionAttributes(
        Map.of("accessToken", provider.createAccessToken(3L, "staff", 1L, 1L)));
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    Message<?> result =
        new JwtChannelInterceptor(
                provider, new JwtAuthenticationConverter(roleId -> RolePermissionInfo.empty()))
            .preSend(message, mock(MessageChannel.class));
    StompHeaderAccessor resultHeaders =
        MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
    assertThat(resultHeaders.getUser().getName()).isEqualTo("staff");
  }
}
