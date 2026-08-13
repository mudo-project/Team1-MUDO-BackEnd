package com.academy.mudogroupware.workspace.domain.repository.comment;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import java.util.Optional;

public interface TaskCommentRepository {

  // id가 null이면 새로 저장하고, 있으면 content·멘션·완료 상태를 반영한다.
  // 멘션은 저장된 댓글의 멘션 목록과 항상 정확히 일치하도록 교체된다.
  TaskComment save(TaskComment comment);

  // 완료 상태(completed/completedBy/completedAt)만 갱신한다. 멘션 테이블은 건드리지 않는다.
  // 토글처럼 멘션이 바뀌지 않는 호출에서 save()를 쓰면 멘션을 delete-then-insert 하다가
  // 유니크 제약 위반이 나므로, 완료 상태만 바꾸는 경로는 반드시 이 메서드를 쓴다.
  TaskComment updateCompletion(TaskComment comment);

  Optional<TaskComment> findById(Long commentId);

  // 하드 삭제. 댓글에 속한 멘션도 함께 제거된다.
  void deleteById(Long commentId);

  // 오래된 댓글부터(등록순) 페이지네이션 조회. 멘션 목록은 채우지 않는다(목록 조회 응답에는 불필요).
  PageResult<TaskComment> findAllByTaskId(Long taskId, int page, int size);
}
