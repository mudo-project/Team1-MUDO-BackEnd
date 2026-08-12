package com.academy.mudogroupware.global.infrastructure.security.websocket;

import com.academy.mudogroupware.global.domain.auth.AuthException;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
  private static final Pattern USER_TOPIC_PATTERN =
      Pattern.compile("^/topic/workspaces/users/(\\d+)$");

  private final JwtTokenProvider provider;
  private final JwtAuthenticationConverter converter;

  public Message<?> preSend(Message<?> m, MessageChannel c) {
    StompHeaderAccessor a = StompHeaderAccessor.wrap(m);
    if (StompCommand.CONNECT.equals(a.getCommand())) {
      Map<String, Object> s = a.getSessionAttributes();
      String t = s == null ? null : (String) s.get("accessToken");
      if (!StringUtils.hasText(t))
        throw new MessageDeliveryException(m, "Access token cookie missing");
      try {
        a.setUser(converter.toAuthentication(provider.parseAccessToken(t)));
        return MessageBuilder.createMessage(m.getPayload(), a.getMessageHeaders());
      } catch (AuthException e) {
        throw new MessageDeliveryException(m, "Invalid JWT token", e);
      }
    }
    if (StompCommand.SUBSCRIBE.equals(a.getCommand())) {
      String destination = a.getDestination();
      Matcher matcher = destination == null ? null : USER_TOPIC_PATTERN.matcher(destination);
      if (matcher != null && matcher.matches()) {
        authorizePersonalTopicSubscription(m, a, matcher.group(1));
      }
    }
    return m;
  }

  private static void authorizePersonalTopicSubscription(
      Message<?> m, StompHeaderAccessor a, String rawUserId) {
    Long targetUserId;
    try {
      targetUserId = Long.valueOf(rawUserId);
    } catch (NumberFormatException e) {
      throw new MessageDeliveryException(m, "Forbidden: invalid user topic id", e);
    }
    if (!(a.getUser() instanceof Authentication authentication)
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof AuthUser me)) {
      throw new MessageDeliveryException(m, "Forbidden: authentication required");
    }
    if (!targetUserId.equals(me.userId())) {
      throw new MessageDeliveryException(m, "Forbidden: cannot subscribe to another user's topic");
    }
  }
}
