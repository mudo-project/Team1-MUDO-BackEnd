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
  // 들어간다. 구독은 화면 진입당 1회뿐이라 정상적인 DB 응답 속도 하에서는 채널 처리 지연이
  // 미미할 것으로 예상되나, DB가 느려지면 그만큼 clientInboundChannel 처리 스레드를 점유한다
  // (쿼리 timeout·지연 metric은 이 프로젝트의 다른 락/조회 지점들과 마찬가지로 프로젝트 전체
  // 정책 이슈로 별도 분리 예정 — 이 메서드만 예외적으로 timeout을 거는 건 비일관적이라 보류).
  // WorkspaceRepository.findById()는 @Transactional(readOnly = true)로 감싸져 있어, 이
  // 인터셉터처럼 트랜잭션 없는 호출자가 불러도 lazy 컬렉션(memberIds) 매핑까지 안전하게
  // 끝난다. DB 조회 실패 시의 fallback은 별도 구현이 필요 없다 — 예외가 preSend() 밖으로
  // 전파되면 Spring이 STOMP ERROR 프레임 + 연결종료로 처리해, 그 자체가 구독 거부다.
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
