package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, Long> {

  boolean existsByAcademyIdAndNameAndDeletedAtIsNull(Long academyId, String name);

  @Query(
      """
      select workspace.id as workspaceId, workspace.name as name, count(member) as memberCount
      from WorkspaceJpaEntity workspace
      left join workspace.members member
      left join WorkspaceRecentAccessJpaEntity recentAccess
          on recentAccess.workspace = workspace and recentAccess.id.userId = :userId
      where workspace.academyId = :academyId
          and workspace.deletedAt is null
          and exists (
              select 1
              from WorkspaceMemberJpaEntity requester
              where requester.workspace = workspace and requester.id.userId = :userId)
      group by workspace.id, workspace.name, workspace.createdAt, recentAccess.lastAccessedAt
      order by case when recentAccess.lastAccessedAt is null then 1 else 0 end,
          recentAccess.lastAccessedAt desc, workspace.createdAt desc, workspace.id desc
      """)
  List<WorkspaceListRow> findMineWorkspaceList(
      @Param("academyId") Long academyId, @Param("userId") Long userId);

  @Query(
      """
      select workspace.id as workspaceId, workspace.name as name, count(member) as memberCount
      from WorkspaceJpaEntity workspace
      left join workspace.members member
      left join WorkspaceRecentAccessJpaEntity recentAccess
          on recentAccess.workspace = workspace and recentAccess.id.userId = :userId
      where workspace.academyId = :academyId and workspace.deletedAt is null
      group by workspace.id, workspace.name, workspace.createdAt, recentAccess.lastAccessedAt
      order by case when recentAccess.lastAccessedAt is null then 1 else 0 end,
          recentAccess.lastAccessedAt desc, workspace.createdAt desc, workspace.id desc
      """)
  List<WorkspaceListRow> findAllWorkspaceList(
      @Param("academyId") Long academyId, @Param("userId") Long userId);

  @Query(
      """
      select count(workspace)
      from WorkspaceJpaEntity workspace
      join workspace.members member
      where workspace.id = :workspaceId
          and workspace.academyId = :academyId
          and workspace.deletedAt is null
          and member.id.userId = :userId
      """)
  long countAccessibleMineWorkspace(
      @Param("workspaceId") Long workspaceId,
      @Param("academyId") Long academyId,
      @Param("userId") Long userId);

  @Query(
      """
      select count(workspace)
      from WorkspaceJpaEntity workspace
      where workspace.id = :workspaceId
          and workspace.academyId = :academyId
          and workspace.deletedAt is null
      """)
  long countActiveWorkspace(
      @Param("workspaceId") Long workspaceId, @Param("academyId") Long academyId);

  @Query(
      """
      select workspace.name
      from WorkspaceJpaEntity workspace
      where workspace.id = :workspaceId and workspace.deletedAt is null
      """)
  Optional<String> findActiveWorkspaceName(@Param("workspaceId") Long workspaceId);

  @Query(
      """
      select member.id.userId
      from WorkspaceMemberJpaEntity member
      where member.workspace.id = :workspaceId
      order by member.id.userId asc
      """)
  List<Long> findMemberUserIds(@Param("workspaceId") Long workspaceId);

  interface WorkspaceListRow {

    Long getWorkspaceId();

    String getName();

    long getMemberCount();
  }
}
