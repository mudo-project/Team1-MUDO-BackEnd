package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.notification.application.port.NotificationUserInfoPort;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.notification.infrastructure.persistence.NotificationJpaRepository;
import com.academy.mudogroupware.notification.infrastructure.persistence.NotificationRepositoryImpl;
import com.academy.mudogroupware.revenuereport.domain.event.RevenueReportGeneratedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
        TimeConfig.class,
        NotificationRepositoryImpl.class,
        NotificationCommandService.class,
        NotificationCreationListener.class,
        NotificationPersistenceTransactionDataJpaTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationPersistenceTransactionDataJpaTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void cleanUp() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    void persistsWorkspaceMentionNotificationAfterOriginalTransactionCommits() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> applicationEventPublisher.publishEvent(
                new TaskCommentMentionedEvent(
                        1L,
                        101L,
                        "상담 일지 작성",
                        501L,
                        10L,
                        List.of(20L),
                        LocalDateTime.of(2026, 8, 15, 10, 0))));

        assertThat(notificationJpaRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(20L);
            assertThat(notification.getType()).isEqualTo(NotificationType.WORKSPACE_TASK_COMMENT_MENTION.name());
            assertThat(notification.getTargetId()).isEqualTo(101L);
        });
    }

    @Test
    void persistsApprovalNotificationAfterOriginalTransactionCommits() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> applicationEventPublisher.publishEvent(
                new ApprovalLineActivatedEvent(
                        200L,
                        "휴가 신청서",
                        30L,
                        LocalDateTime.of(2026, 8, 15, 10, 0))));

        assertThat(notificationJpaRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(30L);
            assertThat(notification.getType()).isEqualTo(NotificationType.APPROVAL_LINE_ACTIVATED.name());
            assertThat(notification.getTargetId()).isEqualTo(200L);
        });
    }

    @Test
    void persistsRevenueReportNotificationWhenEventIsPublishedWithoutTransaction() {
        applicationEventPublisher.publishEvent(new RevenueReportGeneratedEvent(40L, 300L, LocalDate.of(2026, 8, 1)));

        assertThat(notificationJpaRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(40L);
            assertThat(notification.getType()).isEqualTo(NotificationType.REVENUE_REPORT_GENERATED.name());
            assertThat(notification.getTargetId()).isEqualTo(300L);
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        NotificationUserInfoPort notificationUserInfoPort() {
            return userIds -> userIds.stream()
                    .map(userId -> new NotificationUserInfo(userId, "작성자"))
                    .toList();
        }
    }
}
