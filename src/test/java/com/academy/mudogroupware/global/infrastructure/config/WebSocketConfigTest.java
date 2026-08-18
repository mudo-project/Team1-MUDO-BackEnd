package com.academy.mudogroupware.global.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.academy.mudogroupware.global.infrastructure.security.websocket.JwtChannelInterceptor;
import com.academy.mudogroupware.global.infrastructure.security.websocket.JwtHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class WebSocketConfigTest {

  // MessageBrokerRegistry.getSimpleBroker()는 protected다. 실제 설정값(heartbeat,
  // taskScheduler)은 이 메서드가 만들어 반환하는 SimpleBrokerMessageHandler 안에만 있어서,
  // 서브클래스를 만들어 public으로 노출해야 테스트에서 검증할 수 있다.
  private static class ExposedRegistry extends MessageBrokerRegistry {
    ExposedRegistry(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {
      super(clientInboundChannel, clientOutboundChannel);
    }

    SimpleBrokerMessageHandler exposeSimpleBroker() {
      return getSimpleBroker(mock(SubscribableChannel.class));
    }
  }

  @Test
  void configuresTenSecondHeartbeatWithDedicatedScheduler() {
    WebSocketConfig config =
        new WebSocketConfig(mock(JwtChannelInterceptor.class), mock(JwtHandshakeInterceptor.class));
    // 직접 생성한 인스턴스는 Spring 컨테이너를 안 거치므로 @PostConstruct가 자동 실행되지
    // 않는다. 스케줄러가 실제로 초기화됐는지(풀 크기, 스레드 접두사)까지 검증하려면 여기서
    // 직접 호출해야 한다 — 그래야 이 메서드가 실수로 안 불리는 회귀를 테스트가 잡아낸다.
    config.initializeHeartbeatScheduler();
    try {
      ExposedRegistry registry =
          new ExposedRegistry(mock(SubscribableChannel.class), mock(MessageChannel.class));

      config.configureMessageBroker(registry);
      SimpleBrokerMessageHandler handler = registry.exposeSimpleBroker();
      ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) handler.getTaskScheduler();

      assertThat(handler.getHeartbeatValue()).containsExactly(10000L, 10000L);
      // getPoolSize()는 스레드가 실제로 기동된 개수라 초기화 직후엔 0일 수 있다(지연 시작).
      // 설정값 그대로를 보려면 실행자(executor)의 corePoolSize를 확인해야 한다.
      assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("ws-heartbeat-");
    } finally {
      config.shutdownHeartbeatScheduler();
    }
  }
}
