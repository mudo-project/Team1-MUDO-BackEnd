package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskCommentPersistenceAdapter implements TaskCommentRepository {

  private final TaskCommentJpaRepository taskCommentJpaRepository;
  private final TaskCommentMentionJpaRepository taskCommentMentionJpaRepository;
  private final TaskJpaRepository taskJpaRepository;
  private final TaskCommentPersistenceMapper taskCommentPersistenceMapper;

  @Override
  public TaskComment save(TaskComment comment) {
    TaskCommentJpaEntity entity;
    if (comment.getId() == null) {
      TaskJpaEntity task = taskJpaRepository.getReferenceById(comment.getTaskId());
      entity =
          taskCommentJpaRepository.saveAndFlush(
              TaskCommentJpaEntity.create(task, comment.getAuthorId(), comment.getContent()));
    } else {
      entity =
          taskCommentJpaRepository
              .findById(comment.getId())
              .orElseThrow(TaskCommentNotFoundException::new);
      entity.updateContent(comment.getContent());
      syncCompletion(entity, comment);
      taskCommentMentionJpaRepository.deleteAllByCommentId(entity.getId());
    }

    List<TaskCommentMentionJpaEntity> mentionEntities =
        comment.getMentions().stream()
            .map(mention -> TaskCommentMentionJpaEntity.create(entity, mention.getMentionedUserId()))
            .toList();
    taskCommentMentionJpaRepository.saveAll(mentionEntities);

    return taskCommentPersistenceMapper.toDomain(
        entity, taskCommentMentionJpaRepository.findAllByCommentId(entity.getId()));
  }

  @Override
  public Optional<TaskComment> findById(Long commentId) {
    return taskCommentJpaRepository
        .findById(commentId)
        .map(
            entity ->
                taskCommentPersistenceMapper.toDomain(
                    entity, taskCommentMentionJpaRepository.findAllByCommentId(entity.getId())));
  }

  @Override
  public void deleteById(Long commentId) {
    taskCommentMentionJpaRepository.deleteAllByCommentId(commentId);
    taskCommentJpaRepository.deleteById(commentId);
  }

  // 완료 상태가 바뀌었을 때만 엔티티의 도메인 행위를 호출한다(멱등 가드는 엔티티가 이미 갖고 있음).
  private void syncCompletion(TaskCommentJpaEntity entity, TaskComment comment) {
    if (comment.isCompleted() && !entity.isCompleted()) {
      entity.complete(comment.getCompletedBy(), comment.getCompletedAt());
    } else if (!comment.isCompleted() && entity.isCompleted()) {
      entity.cancelCompletion();
    }
  }
}
