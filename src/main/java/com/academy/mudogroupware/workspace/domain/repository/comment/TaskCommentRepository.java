package com.academy.mudogroupware.workspace.domain.repository.comment;

import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import java.util.Optional;

public interface TaskCommentRepository {

  // id가 null이면 새로 저장하고, 있으면 content·멘션·완료 상태를 반영한다.
  // 멘션은 저장된 댓글의 멘션 목록과 항상 정확히 일치하도록 교체된다.
  TaskComment save(TaskComment comment);

  Optional<TaskComment> findById(Long commentId);

  // 하드 삭제. 댓글에 속한 멘션도 함께 제거된다.
  void deleteById(Long commentId);
}
