package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
class WorkspaceRecentAccessMySqlIntegrationTest {

  @Container
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    registry.add("spring.flyway.enabled", () -> false);
  }

  @Autowired private WorkspaceRecentAccessJpaRepository workspaceRecentAccessJpaRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void upsertAllowsConcurrentFirstAccessForSameWorkspace() throws Exception {
    insertWorkspace(100L);
    LocalDateTime accessedAt = LocalDateTime.of(2026, 8, 5, 10, 0);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> first = submitUpsert(executor, ready, start, accessedAt);
      Future<?> second = submitUpsert(executor, ready, start, accessedAt);

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from workspace_recent_access where user_id = ? and workspace_id = ?",
            Integer.class,
            10L,
            100L);
    LocalDateTime lastAccessedAt =
        jdbcTemplate.queryForObject(
            "select last_accessed_at from workspace_recent_access where user_id = ? and workspace_id = ?",
            LocalDateTime.class,
            10L,
            100L);

    assertThat(count).isEqualTo(1);
    assertThat(lastAccessedAt).isEqualTo(accessedAt);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void upsertPreservesNewerAccessTimeWhenOlderRequestArrivesLater() {
    insertWorkspace(101L);
    LocalDateTime newerAccessedAt = LocalDateTime.of(2026, 8, 5, 10, 1);
    LocalDateTime olderAccessedAt = LocalDateTime.of(2026, 8, 5, 10, 0);

    workspaceRecentAccessJpaRepository.upsert(10L, 101L, newerAccessedAt);
    workspaceRecentAccessJpaRepository.upsert(10L, 101L, olderAccessedAt);

    LocalDateTime lastAccessedAt =
        jdbcTemplate.queryForObject(
            "select last_accessed_at from workspace_recent_access where user_id = ? and workspace_id = ?",
            LocalDateTime.class,
            10L,
            101L);

    assertThat(lastAccessedAt).isEqualTo(newerAccessedAt);
  }

  private Future<?> submitUpsert(
      ExecutorService executor,
      CountDownLatch ready,
      CountDownLatch start,
      LocalDateTime accessedAt) {
    return executor.submit(
        () -> {
          ready.countDown();
          start.await();
          workspaceRecentAccessJpaRepository.upsert(10L, 100L, accessedAt);
          return null;
        });
  }

  private void insertWorkspace(long workspaceId) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 9, 0);
    jdbcTemplate.update(
        """
        insert into workspace (workspace_id, name, created_by, created_at, updated_at)
        values (?, ?, ?, ?, ?)
        """,
        workspaceId,
        "workspace-" + workspaceId,
        10L,
        createdAt,
        createdAt);
  }
}
