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
      Long targetUserId = extractUserTopicId(a.getDestination());
      if (targetUserId != null) {
        AuthUser me = (AuthUser) ((Authentication) a.getUser()).getPrincipal();
        if (!targetUserId.equals(me.userId())) {
          throw new MessageDeliveryException(m, "Forbidden: cannot subscribe to another user's topic");
        }
      }
    }
    return m;
  }

  private static Long extractUserTopicId(String destination) {
    if (destination == null) {
      return null;
    }
    Matcher matcher = USER_TOPIC_PATTERN.matcher(destination);
    return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
  }
}
