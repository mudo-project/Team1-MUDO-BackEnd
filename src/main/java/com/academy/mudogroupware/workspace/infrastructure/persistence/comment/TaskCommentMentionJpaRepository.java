package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentMentionJpaRepository
    extends JpaRepository<TaskCommentMentionJpaEntity, Long> {

  List<TaskCommentMentionJpaEntity> findAllByCommentId(Long commentId);

  void deleteAllByCommentId(Long commentId);
}
