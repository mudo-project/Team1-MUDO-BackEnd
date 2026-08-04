package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.global.infrastructure.config.MapStructConfig;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapStructConfig.class)
public interface WorkspacePersistenceMapper {

  WorkspaceJpaEntity toEntity(Workspace workspace);

  @Mapping(target = "memberIds", source = "members", qualifiedByName = "toMemberIds")
  Workspace toDomain(WorkspaceJpaEntity entity);

  @Named("toMemberIds")
  default Set<Long> toMemberIds(List<WorkspaceMemberJpaEntity> members) {
    return members.stream().map(WorkspaceMemberJpaEntity::getUserId).collect(Collectors.toSet());
  }
}
