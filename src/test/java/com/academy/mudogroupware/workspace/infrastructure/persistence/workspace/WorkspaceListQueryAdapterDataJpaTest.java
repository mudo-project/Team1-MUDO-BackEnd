package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, WorkspaceListQueryAdapter.class})
class WorkspaceListQueryAdapterDataJpaTest {

  private static final long ACADEMY_ID = 1L;
  private static final long REQUESTER_ID = 10L;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private WorkspaceListQueryAdapter workspaceListQueryAdapter;

  @Test
  void findMineOrdersRequesterRecentAccessByNewestFirstAndCountsCurrentMembers() {
    insertWorkspace(1L, ACADEMY_ID, "older-access", at(1));
    insertWorkspace(2L, ACADEMY_ID, "newer-access", at(2));
    insertMember(1L, REQUESTER_ID);
    insertMember(1L, 20L);
    insertMember(2L, REQUESTER_ID);
    insertRecentAccess(REQUESTER_ID, 1L, at(4));
    insertRecentAccess(REQUESTER_ID, 2L, at(5));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findMine(ACADEMY_ID, REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId, WorkspaceListItem::memberCount)
        .containsExactly(tuple(2L, 1L), tuple(1L, 2L));
  }

  @Test
  void findMinePlacesUnvisitedRowsAfterVisitedRowsByCreatedAtAndIgnoresAnotherUsersAccess() {
    insertWorkspace(1L, ACADEMY_ID, "visited", at(1));
    insertWorkspace(2L, ACADEMY_ID, "unvisited-old", at(2));
    insertWorkspace(3L, ACADEMY_ID, "unvisited-new", at(3));
    insertWorkspace(4L, ACADEMY_ID, "another-user-access", at(4));
    insertMember(1L, REQUESTER_ID);
    insertMember(2L, REQUESTER_ID);
    insertMember(3L, REQUESTER_ID);
    insertMember(4L, REQUESTER_ID);
    insertRecentAccess(REQUESTER_ID, 1L, at(5));
    insertRecentAccess(99L, 4L, at(6));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findMine(ACADEMY_ID, REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId)
        .containsExactly(1L, 4L, 3L, 2L);
  }

  @Test
  void findAllExcludesDifferentAcademyAndSoftDeletedWorkspaces() {
    insertWorkspace(1L, ACADEMY_ID, "included", at(1));
    insertWorkspace(2L, 2L, "different-academy", at(2));
    insertWorkspace(3L, ACADEMY_ID, "deleted", at(3));
    insertMember(1L, REQUESTER_ID);
    insertMember(1L, 20L);
    insertMember(2L, REQUESTER_ID);
    insertMember(3L, REQUESTER_ID);
    markWorkspaceDeleted(3L, at(4));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(ACADEMY_ID, REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId, WorkspaceListItem::memberCount)
        .containsExactly(tuple(1L, 2L));
  }

  @Test
  void findAllOrdersOnlyRequestersRecentAccessAndIgnoresAnotherUsersAccess() {
    insertWorkspace(1L, ACADEMY_ID, "requester-older-access", at(4));
    insertWorkspace(2L, ACADEMY_ID, "requester-newer-access", at(3));
    insertWorkspace(3L, ACADEMY_ID, "another-user-access", at(2));
    insertWorkspace(4L, ACADEMY_ID, "unvisited", at(1));
    insertRecentAccess(REQUESTER_ID, 1L, at(5));
    insertRecentAccess(REQUESTER_ID, 2L, at(6));
    insertRecentAccess(99L, 3L, at(7));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(ACADEMY_ID, REQUESTER_ID);

    assertThat(result)
        .extracting(WorkspaceListItem::workspaceId)
        .containsExactly(2L, 1L, 3L, 4L);
  }

  @Test
  void findAllUsesWorkspaceIdAsFinalTieBreakerWhenRecentAccessAndCreatedAtMatch() {
    insertWorkspace(1L, ACADEMY_ID, "first", at(1));
    insertWorkspace(2L, ACADEMY_ID, "second", at(1));
    insertRecentAccess(REQUESTER_ID, 1L, at(2));
    insertRecentAccess(REQUESTER_ID, 2L, at(2));

    List<WorkspaceListItem> result = workspaceListQueryAdapter.findAll(ACADEMY_ID, REQUESTER_ID);

    assertThat(result).extracting(WorkspaceListItem::workspaceId).containsExactly(2L, 1L);
  }

  @Test
  void existsAccessibleRequiresMembershipUnlessRequesterCanReadAll() {
    insertWorkspace(1L, ACADEMY_ID, "member", at(1));
    insertWorkspace(2L, ACADEMY_ID, "non-member", at(2));
    insertWorkspace(3L, ACADEMY_ID, "deleted", at(3));
    insertMember(1L, REQUESTER_ID);
    markWorkspaceDeleted(3L, at(4));

    assertThat(workspaceListQueryAdapter.existsAccessible(1L, ACADEMY_ID, REQUESTER_ID, false))
        .isTrue();
    assertThat(workspaceListQueryAdapter.existsAccessible(2L, ACADEMY_ID, REQUESTER_ID, false))
        .isFalse();
    assertThat(workspaceListQueryAdapter.existsAccessible(2L, ACADEMY_ID, REQUESTER_ID, true))
        .isTrue();
    assertThat(workspaceListQueryAdapter.existsAccessible(3L, ACADEMY_ID, REQUESTER_ID, true))
        .isFalse();
  }

  @Test
  void existsAccessibleRejectsWorkspaceFromAnotherAcademyRegardlessOfReadAllPermission() {
    insertWorkspace(1L, 2L, "another-academy", at(1));
    insertMember(1L, REQUESTER_ID);

    assertThat(workspaceListQueryAdapter.existsAccessible(1L, ACADEMY_ID, REQUESTER_ID, false))
        .isFalse();
    assertThat(workspaceListQueryAdapter.existsAccessible(1L, ACADEMY_ID, REQUESTER_ID, true))
        .isFalse();
  }

  private void insertWorkspace(
      long workspaceId, long academyId, String name, LocalDateTime createdAt) {
    jdbcTemplate.update(
        """
        insert into workspace (workspace_id, academy_id, name, created_by, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?)
        """,
        workspaceId,
        academyId,
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
