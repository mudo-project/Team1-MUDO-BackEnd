package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskCommentMention;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
      // 댓글 저장
      entity = taskCommentJpaRepository.saveAndFlush(
              TaskCommentJpaEntity.create(
                      task,
                      comment.getAuthorId(),
                      comment.getContent()
              )
          );
      insertMentions(entity, comment.getMentions());
    } else {
      entity =
          taskCommentJpaRepository
              .findById(comment.getId())
              .orElseThrow(TaskCommentNotFoundException::new);
      entity.updateContent(comment.getContent());
      syncCompletion(entity, comment);
      syncMentions(entity, comment.getMentions());
    }

    return taskCommentPersistenceMapper.toDomain(
        entity, taskCommentMentionJpaRepository.findAllByCommentId(entity.getId()));
  }

  @Override
  public TaskComment updateCompletion(TaskComment comment) {
    TaskCommentJpaEntity entity =
        taskCommentJpaRepository
            .findById(comment.getId())
            .orElseThrow(TaskCommentNotFoundException::new);
    syncCompletion(entity, comment);
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

  @Override
  public PageResult<TaskComment> findAllByTaskId(Long taskId, int page, int size) {
    Slice<TaskCommentJpaEntity> slice =
        taskCommentJpaRepository.findAllByTaskId(taskId, PageRequest.of(page, size));
    List<TaskComment> content =
        slice.getContent().stream()
            .map(entity -> taskCommentPersistenceMapper.toDomain(entity, List.of()))
            .toList();
    return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
  }

  private void insertMentions(TaskCommentJpaEntity entity, List<TaskCommentMention> mentions) {
    List<TaskCommentMentionJpaEntity> mentionEntities =
        mentions.stream()
            .map(mention -> TaskCommentMentionJpaEntity.create(entity, mention.getMentionedUserId()))
            .toList();
    taskCommentMentionJpaRepository.saveAll(mentionEntities);
  }

  // 기존 멘션과 새 멘션의 사용자 ID 집합을 비교해, 제거된 것만 삭제하고 추가된 것만 insert한다.
  // 겹치는 멘션은 delete/insert 어느 쪽도 타지 않으므로, Hibernate가 같은 트랜잭션에서
  // insert를 delete보다 먼저 flush하는 순서와 IDENTITY 생성 전략이 겹쳐도 유니크 제약을
  // 위반할 여지가 없다(이슈 #462/PR #463에서 토글 경로에 났던 것과 동일 계열 버그의 근본 예방).
  private void syncMentions(TaskCommentJpaEntity entity, List<TaskCommentMention> mentions) {
    Set<Long> existingUserIds =
        taskCommentMentionJpaRepository.findAllByCommentId(entity.getId()).stream()
            .map(TaskCommentMentionJpaEntity::getMentionedUserId)
            .collect(Collectors.toSet());
    Set<Long> newUserIds =
        mentions.stream().map(TaskCommentMention::getMentionedUserId).collect(Collectors.toSet());

    Set<Long> toRemove = new HashSet<>(existingUserIds);
    toRemove.removeAll(newUserIds);
    if (!toRemove.isEmpty()) {
      taskCommentMentionJpaRepository.deleteAllByCommentIdAndMentionedUserIdIn(entity.getId(), toRemove);
    }

    List<TaskCommentMention> toAdd =
        mentions.stream().filter(m -> !existingUserIds.contains(m.getMentionedUserId())).toList();
    if (!toAdd.isEmpty()) {
      insertMentions(entity, toAdd);
    }
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
