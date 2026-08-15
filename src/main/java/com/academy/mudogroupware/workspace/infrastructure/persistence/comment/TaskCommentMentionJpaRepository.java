package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentMentionJpaRepository
    extends JpaRepository<TaskCommentMentionJpaEntity, Long> {

  List<TaskCommentMentionJpaEntity> findAllByCommentId(Long commentId);

  void deleteAllByCommentId(Long commentId);

  // 댓글 수정 시 diff 기반 멘션 동기화에 쓰인다 — 제거 대상 사용자 ID만 골라 지운다.
  void deleteAllByCommentIdAndMentionedUserIdIn(Long commentId, Collection<Long> mentionedUserIds);
}
