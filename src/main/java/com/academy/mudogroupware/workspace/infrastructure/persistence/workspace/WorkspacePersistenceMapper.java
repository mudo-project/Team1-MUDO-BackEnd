package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.global.infrastructure.config.MapStructConfig;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface WorkspacePersistenceMapper {

  WorkspaceJpaEntity toEntity(Workspace workspace);

  default Workspace toDomain(WorkspaceJpaEntity entity) {
    return Workspace.restore(
        entity.getId(),
        entity.getAcademyId(),
        entity.getName(),
        entity.getCreatedBy(),
        toMemberIds(entity.getMembers()));
  }

  default Set<Long> toMemberIds(List<WorkspaceMemberJpaEntity> members) {
    return members.stream()
        .map(WorkspaceMemberJpaEntity::getUserId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
