package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import com.academy.mudogroupware.global.infrastructure.security.websocket.JwtChannelInterceptor;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

class JwtChannelInterceptorTest {
  @Test
  void authenticatesConnectCookieToken() {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    JwtTokenProvider provider = new JwtTokenProvider(p);
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
    headers.setSessionAttributes(
        Map.of("accessToken", provider.createAccessToken(3L, "staff", 5L, AccountType.MEMBER, null, false)));
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    Message<?> result =
        new JwtChannelInterceptor(
                provider,
                new JwtAuthenticationConverter(
                    roleId -> new RolePermissionInfo("STAFF", Set.of("CHAT:SEND")),
                    Set::of))
            .preSend(message, mock(MessageChannel.class));
    StompHeaderAccessor resultHeaders =
        MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
    Principal user = resultHeaders.getUser();
    assertThat(user.getName()).isEqualTo("staff");
    Authentication authentication = (Authentication) user;
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("CHAT:SEND");
  }
}
