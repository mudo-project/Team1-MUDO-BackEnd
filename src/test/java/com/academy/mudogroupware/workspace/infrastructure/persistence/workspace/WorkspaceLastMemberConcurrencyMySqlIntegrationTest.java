package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.application.command.workspace.RemoveWorkspaceMemberCommand;
import com.academy.mudogroupware.workspace.application.service.workspace.RemoveWorkspaceMemberService;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceLastMemberException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@Import({TimeConfig.class, WorkspacePersistenceAdapter.class, WorkspacePersistenceMapperImpl.class,
    RemoveWorkspaceMemberService.class})
class WorkspaceLastMemberConcurrencyMySqlIntegrationTest {

  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    registry.add("spring.flyway.enabled", () -> false);
  }

  @Autowired private RemoveWorkspaceMemberService removeWorkspaceMemberService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void onlyOneOfTwoConcurrentLastPairRemovalsSucceeds() throws Exception {
    insertWorkspaceWithTwoMembers(200L, 10L, 20L);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger lastMemberRejectionCount = new AtomicInteger();

    try {
      Future<?> first = submitRemoval(executor, ready, start, 200L, 10L, successCount, lastMemberRejectionCount);
      Future<?> second = submitRemoval(executor, ready, start, 200L, 20L, successCount, lastMemberRejectionCount);

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(lastMemberRejectionCount.get()).isEqualTo(1);
    Integer remainingMembers =
        jdbcTemplate.queryForObject(
            "select count(*) from workspace_member where workspace_id = ?", Integer.class, 200L);
    assertThat(remainingMembers).isEqualTo(1);
  }

  private Future<?> submitRemoval(
      ExecutorService executor,
      CountDownLatch ready,
      CountDownLatch start,
      Long workspaceId,
      Long targetUserId,
      AtomicInteger successCount,
      AtomicInteger lastMemberRejectionCount) {
    return executor.submit(
        () -> {
          ready.countDown();
          start.await();
          try {
            removeWorkspaceMemberService.removeMember(
                new RemoveWorkspaceMemberCommand(targetUserId, workspaceId, targetUserId));
            successCount.incrementAndGet();
          } catch (WorkspaceLastMemberException exception) {
            lastMemberRejectionCount.incrementAndGet();
          }
          return null;
        });
  }

  private void insertWorkspaceWithTwoMembers(long workspaceId, long userId1, long userId2) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 9, 0);
    jdbcTemplate.update(
        """
        insert into workspace (workspace_id, name, created_by, created_at, updated_at)
        values (?, ?, ?, ?, ?)
        """,
        workspaceId, "workspace", userId1, createdAt, createdAt);
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId, userId1, createdAt);
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId, userId2, createdAt);
  }
}
