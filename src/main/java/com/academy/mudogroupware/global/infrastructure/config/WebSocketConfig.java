package com.academy.mudogroupware.global.infrastructure.config;

import com.academy.mudogroupware.global.infrastructure.security.websocket.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtChannelInterceptor channel;
  private final JwtHandshakeInterceptor handshake;

  // 죽은 WebSocket 연결이 heartbeat 무응답으로 감지·정리되도록 전용 스케줄러를 둔다.
  // 기존 @Scheduled 작업(SchedulingConfig)과 스레드 풀을 공유하지 않기 위해 별도로 만든다.
  private final ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();

  @Value("${WEBSOCKET_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}")
  private String origins;

  @PostConstruct
  void initializeHeartbeatScheduler() {
    heartbeatScheduler.setPoolSize(1);
    heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
    heartbeatScheduler.initialize();
  }

  @PreDestroy
  void shutdownHeartbeatScheduler() {
    heartbeatScheduler.shutdown();
  }

  public void registerStompEndpoints(StompEndpointRegistry r) {
    String[] allowed = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    r.addEndpoint("/ws").setAllowedOriginPatterns(allowed).addInterceptors(handshake).withSockJS();
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    // 서버가 10초마다 ping을 보내고, 클라이언트로부터 10초 안에 응답이 없으면 죽은 연결로
    // 판단해 소켓을 닫는다. 프론트 STOMP 클라이언트도 heartbeat를 0이 아닌 값으로 선언해야
    // 협상이 성립한다(양쪽 다 설정 필요) — 프론트 저장소는 이 변경 범위 밖.
    r.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10000, 10000})
        .setTaskScheduler(heartbeatScheduler);
    r.setApplicationDestinationPrefixes("/app");
    r.setUserDestinationPrefix("/user");
  }

  public void configureClientInboundChannel(ChannelRegistration r) {
    r.interceptors(channel);
  }
}
