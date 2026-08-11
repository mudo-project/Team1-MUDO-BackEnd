package com.academy.mudogroupware.google.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;

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

import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;

// @TransactionalEventListener(phase = AFTER_COMMIT)는 Spring의 실제 트랜잭션 동기화 메커니즘에
// 의존하므로, 리스너 메서드를 직접 호출하는 단위 테스트로는 "커밋 전 미실행"·"롤백 시 미실행"을
// 검증할 수 없다. 이 테스트는 실제 PlatformTransactionManager + ApplicationEventPublisher를 갖춘
// 최소 Spring 컨텍스트로 그 계약을 직접 검증한다. JPA/엔티티는 필요 없어 인메모리 H2를 트랜잭션
// 리소스로만 사용한다.
@SpringJUnitConfig(GoogleTokenRevocationListenerTransactionIntegrationTest.Config.class)
class GoogleTokenRevocationListenerTransactionIntegrationTest {

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
        GoogleOAuthPort googleOAuthPort() {
            return mock(GoogleOAuthPort.class);
        }

        @Bean
        GoogleTokenRevocationListener googleTokenRevocationListener(GoogleOAuthPort googleOAuthPort) {
            return new GoogleTokenRevocationListener(googleOAuthPort);
        }
    }

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private GoogleOAuthPort googleOAuthPort;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void doesNotRevokeBeforeCommitAndRevokesAfterCommit() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OldGoogleRefreshTokenRevocationRequestedEvent("token-committed"));
            verify(googleOAuthPort, never()).revoke(any());
        });

        verify(googleOAuthPort).revoke("token-committed");
    }

    @Test
    void doesNotRevokeWhenTransactionRollsBack() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OldGoogleRefreshTokenRevocationRequestedEvent("token-rolled-back"));
            status.setRollbackOnly();
        });

        verifyNoInteractions(googleOAuthPort);
    }
}
