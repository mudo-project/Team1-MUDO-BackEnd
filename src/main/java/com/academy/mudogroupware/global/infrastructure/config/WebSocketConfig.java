package com.academy.mudogroupware.global.infrastructure.config;

import com.academy.mudogroupware.global.infrastructure.security.websocket.*;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtChannelInterceptor channel;
  private final JwtHandshakeInterceptor handshake;

  @Value("${WEBSOCKET_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}")
  private String origins;

  public void registerStompEndpoints(StompEndpointRegistry r) {
    String[] allowed = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    r.addEndpoint("/ws").setAllowedOriginPatterns(allowed).addInterceptors(handshake).withSockJS();
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    r.enableSimpleBroker("/topic", "/queue");
    r.setApplicationDestinationPrefixes("/app");
    r.setUserDestinationPrefix("/user");
  }

  public void configureClientInboundChannel(ChannelRegistration r) {
    r.interceptors(channel);
  }
}
