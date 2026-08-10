package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TimeConfig.class)
class WorkspaceJpaRepositoryDetailQueryDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private WorkspaceJpaRepository workspaceJpaRepository;

  @Test
  void findActiveWorkspaceNameReturnsEmptyWhenDeleted() {
    insertWorkspace(1L, "active", at());
    insertWorkspace(2L, "another", at());
    insertWorkspace(3L, "deleted", at());
    jdbcTemplate.update("update workspace set deleted_at = ? where workspace_id = 3", at());

    assertThat(workspaceJpaRepository.findActiveWorkspaceName(1L)).contains("active");
    assertThat(workspaceJpaRepository.findActiveWorkspaceName(2L)).contains("another");
    assertThat(workspaceJpaRepository.findActiveWorkspaceName(3L)).isEmpty();
    assertThat(workspaceJpaRepository.findActiveWorkspaceName(999L)).isEmpty();
  }

  @Test
  void findMemberUserIdsReturnsAllMembersOfTheWorkspaceOrderedByUserId() {
    insertWorkspace(1L, "ws", at());
    insertMember(1L, 20L);
    insertMember(1L, 10L);

    List<Long> result = workspaceJpaRepository.findMemberUserIds(1L);

    assertThat(result).containsExactly(10L, 20L);
  }

  private void insertWorkspace(long workspaceId, String name, LocalDateTime at) {
    jdbcTemplate.update(
        "insert into workspace (workspace_id, name, created_by, created_at, updated_at) "
            + "values (?, ?, 10, ?, ?)",
        workspaceId,
        name,
        at,
        at);
  }

  private void insertMember(long workspaceId, long userId) {
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId,
        userId,
        at());
  }

  private LocalDateTime at() {
    return LocalDateTime.of(2026, 8, 1, 9, 0);
  }
}
