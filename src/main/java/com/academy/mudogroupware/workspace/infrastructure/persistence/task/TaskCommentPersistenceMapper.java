package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.config.MapStructConfig;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.TaskCommentMention;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface TaskCommentPersistenceMapper {

  default TaskComment toDomain(
      TaskCommentJpaEntity entity, List<TaskCommentMentionJpaEntity> mentionEntities) {
    List<TaskCommentMention> mentions =
        mentionEntities.stream()
            .map(m -> TaskCommentMention.restore(m.getId(), m.getMentionedUserId(), m.getCreatedAt()))
            .toList();
    return TaskComment.restore(
        entity.getId(),
        entity.getTask().getId(),
        entity.getAuthorId(),
        entity.getContent(),
        entity.isCompleted(),
        entity.getCompletedBy(),
        entity.getCompletedAt(),
        mentions,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
