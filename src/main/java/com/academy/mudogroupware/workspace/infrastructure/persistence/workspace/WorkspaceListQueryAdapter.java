package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceListQueryAdapter implements WorkspaceListQueryPort {

  private static final String ORDER_BY_RECENT_ACCESS =
      """
      group by workspace.id, workspace.name, workspace.createdAt, recentAccess.lastAccessedAt
      order by case when recentAccess.lastAccessedAt is null then 1 else 0 end,
          recentAccess.lastAccessedAt desc, workspace.createdAt desc
      """;

  private final EntityManager entityManager;

  @Override
  public List<WorkspaceListItem> findMine(Long academyId, Long userId) {
    return entityManager
        .createQuery(
            """
            select new com.academy.mudogroupware.workspace.application.query.WorkspaceListItem(
                workspace.id, workspace.name, count(member))
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
            """
                + ORDER_BY_RECENT_ACCESS,
            WorkspaceListItem.class)
        .setParameter("academyId", academyId)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public List<WorkspaceListItem> findAll(Long academyId, Long userId) {
    return entityManager
        .createQuery(
            """
            select new com.academy.mudogroupware.workspace.application.query.WorkspaceListItem(
                workspace.id, workspace.name, count(member))
            from WorkspaceJpaEntity workspace
            left join workspace.members member
            left join WorkspaceRecentAccessJpaEntity recentAccess
                on recentAccess.workspace = workspace and recentAccess.id.userId = :userId
            where workspace.academyId = :academyId and workspace.deletedAt is null
            """
                + ORDER_BY_RECENT_ACCESS,
            WorkspaceListItem.class)
        .setParameter("academyId", academyId)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public boolean existsAccessible(Long workspaceId, Long academyId, Long userId, boolean canReadAll) {
    if (canReadAll) {
      return existsActiveWorkspace(workspaceId, academyId);
    }

    Long count =
        entityManager
            .createQuery(
                """
                select count(workspace)
                from WorkspaceJpaEntity workspace
                join workspace.members member
                where workspace.id = :workspaceId
                    and workspace.academyId = :academyId
                    and workspace.deletedAt is null
                    and member.id.userId = :userId
                """,
                Long.class)
            .setParameter("workspaceId", workspaceId)
            .setParameter("academyId", academyId)
            .setParameter("userId", userId)
            .getSingleResult();
    return count > 0;
  }

  private boolean existsActiveWorkspace(Long workspaceId, Long academyId) {
    Long count =
        entityManager
            .createQuery(
                """
                select count(workspace)
                from WorkspaceJpaEntity workspace
                where workspace.id = :workspaceId
                    and workspace.academyId = :academyId
                    and workspace.deletedAt is null
                """,
                Long.class)
            .setParameter("workspaceId", workspaceId)
            .setParameter("academyId", academyId)
            .getSingleResult();
    return count > 0;
  }
}
