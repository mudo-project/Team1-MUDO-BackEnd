package com.academy.mudogroupware.global.infrastructure.security.websocket;

import com.academy.mudogroupware.global.domain.auth.AuthException;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
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
  private static final Pattern WORKSPACE_TOPIC_PATTERN =
      Pattern.compile("^/topic/workspaces/(\\d+)$");

  private final JwtTokenProvider provider;
  private final JwtAuthenticationConverter converter;
  private final WorkspaceRepository workspaceRepository;

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
      Matcher userMatcher = destination == null ? null : USER_TOPIC_PATTERN.matcher(destination);
      if (userMatcher != null && userMatcher.matches()) {
        authorizePersonalTopicSubscription(m, a, userMatcher.group(1));
      }
      Matcher workspaceMatcher =
          destination == null ? null : WORKSPACE_TOPIC_PATTERN.matcher(destination);
      if (workspaceMatcher != null && workspaceMatcher.matches()) {
        authorizeWorkspaceTopicSubscription(m, a, workspaceMatcher.group(1));
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
    AuthUser me = requireAuthenticatedUser(m, a);
    if (!targetUserId.equals(me.userId())) {
      throw new MessageDeliveryException(m, "Forbidden: cannot subscribe to another user's topic");
    }
  }

  // 워크스페이스 토픽은 참여자만 구독 가능하다 — 참여자 확인을 위해 처음으로 DB 조회가
  // 들어간다(구독은 화면 진입당 1회뿐이라 성능 영향 없음).
  private void authorizeWorkspaceTopicSubscription(
      Message<?> m, StompHeaderAccessor a, String rawWorkspaceId) {
    Long workspaceId;
    try {
      workspaceId = Long.valueOf(rawWorkspaceId);
    } catch (NumberFormatException e) {
      throw new MessageDeliveryException(m, "Forbidden: invalid workspace topic id", e);
    }
    AuthUser me = requireAuthenticatedUser(m, a);
    Workspace workspace =
        workspaceRepository
            .findById(workspaceId)
            .orElseThrow(() -> new MessageDeliveryException(m, "Forbidden: workspace not found"));
    if (!workspace.getMemberIds().contains(me.userId())) {
      throw new MessageDeliveryException(m, "Forbidden: not a workspace member");
    }
  }

  private static AuthUser requireAuthenticatedUser(Message<?> m, StompHeaderAccessor a) {
    if (!(a.getUser() instanceof Authentication authentication)
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof AuthUser me)) {
      throw new MessageDeliveryException(m, "Forbidden: authentication required");
    }
    return me;
  }
}
