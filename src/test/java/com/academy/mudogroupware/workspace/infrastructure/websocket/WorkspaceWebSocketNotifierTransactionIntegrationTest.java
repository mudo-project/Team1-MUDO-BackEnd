package com.academy.mudogroupware.workspace.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(WorkspaceWebSocketNotifierTransactionIntegrationTest.Config.class)
class WorkspaceWebSocketNotifierTransactionIntegrationTest {

  @Configuration
  @EnableTransactionManagement
  static class Config {

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    WebSocketEventPublisher webSocketEventPublisher() {
      return mock(WebSocketEventPublisher.class);
    }

    @Bean
    WorkspaceWebSocketNotifier workspaceWebSocketNotifier(
        WebSocketEventPublisher webSocketEventPublisher) {
      return new WorkspaceWebSocketNotifier(webSocketEventPublisher);
    }
  }

  @Autowired private ApplicationEventPublisher applicationEventPublisher;
  @Autowired private WebSocketEventPublisher webSocketEventPublisher;
  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void resetPublisher() {
    reset(webSocketEventPublisher);
  }

  @Test
  void publishesOnlyAfterTransactionCommits() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    TaskCommentMentionedEvent event = eventFor(11L);

    transactionTemplate.executeWithoutResult(
        status -> {
          applicationEventPublisher.publishEvent(event);
          verify(webSocketEventPublisher, never()).publish(any(), any());
        });

    verify(webSocketEventPublisher)
        .publish(
            eq("/topic/workspaces/users/11"),
            any(TaskCommentMentionedSocketResponse.class));
  }

  @Test
  void doesNotPublishWhenTransactionRollsBack() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    transactionTemplate.executeWithoutResult(
        status -> {
          applicationEventPublisher.publishEvent(eventFor(11L));
          status.setRollbackOnly();
        });

    verifyNoInteractions(webSocketEventPublisher);
  }

  private TaskCommentMentionedEvent eventFor(Long recipientUserId) {
    return new TaskCommentMentionedEvent(
        1L,
        101L,
        "상담 일지 작성",
        501L,
        10L,
        List.of(recipientUserId),
        LocalDateTime.of(2026, 8, 12, 10, 30));
  }
}
