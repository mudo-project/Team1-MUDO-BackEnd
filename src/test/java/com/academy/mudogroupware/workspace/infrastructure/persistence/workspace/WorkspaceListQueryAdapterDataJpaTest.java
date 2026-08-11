package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, WorkspaceListQueryAdapter.class})
class WorkspaceListQueryAdapterDataJpaTest {

  private static final long REQUESTER_ID = 10L;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private WorkspaceListQueryAdapter workspaceListQueryAdapter;

  @Test
  void findMineOrdersRequesterRecentAccessByNewestFirstAndCountsCurrentMembers() {
    insertWorkspace(1L, "older-access", at(1));
    insertWorkspace(2L, "newer-access", at(2));
    insertMember(1L, REQUESTER_ID);
    insertMember(1L, 20L);
    insertMember(2L, REQUESTER_ID);
    insertRecentAccess(REQUESTER_ID, 1L, at(4));
    insertRecentAccess(REQUESTER_ID, 2L, at(5));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findMine(REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId, WorkspaceListItem::memberCount)
        .containsExactly(tuple(2L, 1L), tuple(1L, 2L));
  }

  @Test
  void findMinePlacesUnvisitedRowsAfterVisitedRowsByCreatedAtAndIgnoresAnotherUsersAccess() {
    insertWorkspace(1L, "visited", at(1));
    insertWorkspace(2L, "unvisited-old", at(2));
    insertWorkspace(3L, "unvisited-new", at(3));
    insertWorkspace(4L, "another-user-access", at(4));
    insertMember(1L, REQUESTER_ID);
    insertMember(2L, REQUESTER_ID);
    insertMember(3L, REQUESTER_ID);
    insertMember(4L, REQUESTER_ID);
    insertRecentAccess(REQUESTER_ID, 1L, at(5));
    insertRecentAccess(99L, 4L, at(6));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findMine(REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId)
        .containsExactly(1L, 4L, 3L, 2L);
  }

  @Test
  void findAllExcludesSoftDeletedWorkspaces() {
    insertWorkspace(1L, "included", at(1));
    insertWorkspace(3L, "deleted", at(3));
    insertMember(1L, REQUESTER_ID);
    insertMember(1L, 20L);
    insertMember(3L, REQUESTER_ID);
    markWorkspaceDeleted(3L, at(4));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId, WorkspaceListItem::memberCount)
        .containsExactly(tuple(1L, 2L));
  }

  @Test
  void findAllOrdersOnlyRequestersRecentAccessAndIgnoresAnotherUsersAccess() {
    insertWorkspace(1L, "requester-older-access", at(4));
    insertWorkspace(2L, "requester-newer-access", at(3));
    insertWorkspace(3L, "another-user-access", at(2));
    insertWorkspace(4L, "unvisited", at(1));
    insertRecentAccess(REQUESTER_ID, 1L, at(5));
    insertRecentAccess(REQUESTER_ID, 2L, at(6));
    insertRecentAccess(99L, 3L, at(7));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId)
        .containsExactly(2L, 1L, 3L, 4L);
  }

  @Test
  void findAllUsesWorkspaceIdAsFinalTieBreakerWhenRecentAccessAndCreatedAtMatch() {
    insertWorkspace(1L, "first", at(1));
    insertWorkspace(2L, "second", at(1));
    insertRecentAccess(REQUESTER_ID, 1L, at(2));
    insertRecentAccess(REQUESTER_ID, 2L, at(2));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(REQUESTER_ID);

    assertThat(result).extracting(WorkspaceListItem::workspaceId).containsExactly(2L, 1L);
  }

  @Test
  void existsAccessibleRequiresMembershipUnlessRequesterCanReadAll() {
    insertWorkspace(1L, "member", at(1));
    insertWorkspace(2L, "non-member", at(2));
    insertWorkspace(3L, "deleted", at(3));
    insertMember(1L, REQUESTER_ID);
    markWorkspaceDeleted(3L, at(4));

    assertThat(workspaceListQueryAdapter.existsAccessible(1L, REQUESTER_ID, false)).isTrue();
    assertThat(workspaceListQueryAdapter.existsAccessible(2L, REQUESTER_ID, false)).isFalse();
    assertThat(workspaceListQueryAdapter.existsAccessible(2L, REQUESTER_ID, true)).isTrue();
    assertThat(workspaceListQueryAdapter.existsAccessible(3L, REQUESTER_ID, true)).isFalse();
  }

  private void insertWorkspace(long workspaceId, String name, LocalDateTime createdAt) {
    jdbcTemplate.update(
        """
        insert into workspace (workspace_id, name, created_by, created_at, updated_at)
        values (?, ?, ?, ?, ?)
        """,
        workspaceId,
        name,
        REQUESTER_ID,
        createdAt,
        createdAt);
  }

  private void insertMember(long workspaceId, long userId) {
    jdbcTemplate.update(
        "insert into workspace_member (workspace_id, user_id, created_at) values (?, ?, ?)",
        workspaceId,
        userId,
        at(1));
  }

  private void insertRecentAccess(long userId, long workspaceId, LocalDateTime lastAccessedAt) {
    jdbcTemplate.update(
        """
        insert into workspace_recent_access (user_id, workspace_id, last_accessed_at)
        values (?, ?, ?)
        """,
        userId,
        workspaceId,
        lastAccessedAt);
  }

  private void markWorkspaceDeleted(long workspaceId, LocalDateTime deletedAt) {
    jdbcTemplate.update("update workspace set deleted_at = ? where workspace_id = ?", deletedAt, workspaceId);
  }

  private LocalDateTime at(int day) {
    return LocalDateTime.of(2026, 8, day, 9, 0);
  }
}
