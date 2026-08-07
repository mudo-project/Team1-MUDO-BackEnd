package com.academy.mudogroupware.workspace.domain.repository;

import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import java.util.Optional;

public interface TaskCommentRepository {

  // id가 null이면 새로 저장하고, 있으면 content·멘션·완료 상태를 반영한다.
  // 멘션은 매번 전체 삭제 후 재삽입한다(diff 없음).
  TaskComment save(TaskComment comment);

  Optional<TaskComment> findById(Long commentId);

  // 하드 삭제. 멘션은 FK ON DELETE CASCADE로 함께 제거된다.
  void deleteById(Long commentId);
}
