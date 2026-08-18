package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import com.academy.mudogroupware.global.infrastructure.security.websocket.JwtChannelInterceptor;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

class JwtChannelInterceptorTest {
  // 기존 시그니처 유지 + 새 오버로드 추가 (기존 6개 호출부는 그대로 컴파일된다)
  private static JwtChannelInterceptor newInterceptor() {
    return newInterceptor(mock(WorkspaceRepository.class));
  }

  private static JwtChannelInterceptor newInterceptor(WorkspaceRepository workspaceRepository) {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    return new JwtChannelInterceptor(
        new JwtTokenProvider(p),
        new JwtAuthenticationConverter(
            roleId -> new RolePermissionInfo("STAFF", Set.of("CHAT:SEND")), Set::of),
        workspaceRepository);
  }

  private static Authentication authenticationOf(long userId) {
    AuthUser user = new AuthUser(userId, "user-" + userId, 5L, "STAFF");
    return new UsernamePasswordAuthenticationToken(user, null, Set.of());
  }

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
                    Set::of),
                mock(WorkspaceRepository.class))
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

  @Test
  void rejectsSubscribingToAnotherUsersTopic() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/users/11");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void allowsSubscribingToOwnUserTopic() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/users/10");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    Message<?> result = newInterceptor().preSend(message, mock(MessageChannel.class));

    assertThat(result).isSameAs(message);
  }

  @Test
  void ignoresDestinationsThatAreNotPersonalUserTopics() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/other");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    Message<?> result = newInterceptor().preSend(message, mock(MessageChannel.class));

    assertThat(result).isSameAs(message);
  }

  @Test
  void rejectsUserTopicIdThatOverflowsLong() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/users/99999999999999999999");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void rejectsPersonalTopicSubscriptionWithoutAuthenticatedUser() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/users/10");
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void rejectsPersonalTopicSubscriptionWithUnauthenticatedPrincipal() {
    AuthUser user = new AuthUser(10L, "user-10", 5L, "STAFF");
    Authentication unauthenticated = new UsernamePasswordAuthenticationToken(user, "credentials");
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/users/10");
    headers.setUser(unauthenticated);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void propagatesConnectAuthenticationToSubsequentSubscribe() {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    JwtTokenProvider provider = new JwtTokenProvider(p);
    JwtChannelInterceptor interceptor =
        new JwtChannelInterceptor(
            provider,
            new JwtAuthenticationConverter(
                roleId -> new RolePermissionInfo("STAFF", Set.of("CHAT:SEND")), Set::of),
            mock(WorkspaceRepository.class));

    StompHeaderAccessor connectHeaders = StompHeaderAccessor.create(StompCommand.CONNECT);
    connectHeaders.setSessionAttributes(
        Map.of("accessToken", provider.createAccessToken(10L, "staff", 5L, AccountType.MEMBER, null, false)));
    Message<byte[]> connectMessage =
        MessageBuilder.createMessage(new byte[0], connectHeaders.getMessageHeaders());
    Message<?> connectResult = interceptor.preSend(connectMessage, mock(MessageChannel.class));
    Principal authenticatedUser =
        MessageHeaderAccessor.getAccessor(connectResult, StompHeaderAccessor.class).getUser();

    StompHeaderAccessor subscribeOwnTopic = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    subscribeOwnTopic.setDestination("/topic/workspaces/users/10");
    subscribeOwnTopic.setUser(authenticatedUser);
    Message<byte[]> subscribeOwnMessage =
        MessageBuilder.createMessage(new byte[0], subscribeOwnTopic.getMessageHeaders());

    assertThat(interceptor.preSend(subscribeOwnMessage, mock(MessageChannel.class)))
        .isSameAs(subscribeOwnMessage);

    StompHeaderAccessor subscribeOtherTopic = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    subscribeOtherTopic.setDestination("/topic/workspaces/users/99");
    subscribeOtherTopic.setUser(authenticatedUser);
    Message<byte[]> subscribeOtherMessage =
        MessageBuilder.createMessage(new byte[0], subscribeOtherTopic.getMessageHeaders());

    assertThatThrownBy(() -> interceptor.preSend(subscribeOtherMessage, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void allowsSubscribingToWorkspaceTopicWhenMember() {
    WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    when(workspaceRepository.findById(2L))
        .thenReturn(Optional.of(Workspace.restore(2L, "팀", 1L, Set.of(10L))));
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/2");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    Message<?> result = newInterceptor(workspaceRepository).preSend(message, mock(MessageChannel.class));

    assertThat(result).isSameAs(message);
  }

  @Test
  void rejectsSubscribingToWorkspaceTopicWhenNotMember() {
    WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    when(workspaceRepository.findById(2L))
        .thenReturn(Optional.of(Workspace.restore(2L, "팀", 1L, Set.of(10L))));
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/2");
    headers.setUser(authenticationOf(99L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(
            () -> newInterceptor(workspaceRepository).preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void rejectsSubscribingToNonexistentWorkspaceTopic() {
    WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    when(workspaceRepository.findById(2L)).thenReturn(Optional.empty());
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/2");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(
            () -> newInterceptor(workspaceRepository).preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void rejectsWorkspaceTopicIdThatOverflowsLong() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/99999999999999999999");
    headers.setUser(authenticationOf(10L));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }

  @Test
  void rejectsWorkspaceTopicSubscriptionWithoutAuthenticatedUser() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination("/topic/workspaces/2");
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

    assertThatThrownBy(() -> newInterceptor().preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class);
  }
}
