package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentJpaRepository extends JpaRepository<TaskCommentJpaEntity, Long> {

  @Query(
      """
      select comment.task.id as taskId,
          sum(case when comment.completed = true then 1 else 0 end) as completedCount,
          count(comment) as totalCount
      from TaskCommentJpaEntity comment
      where comment.task.id in :taskIds
      group by comment.task.id
      """)
  List<TaskCommentSummaryRow> summarizeByTaskIds(@Param("taskIds") List<Long> taskIds);

  interface TaskCommentSummaryRow {

    Long getTaskId();

    long getCompletedCount();

    long getTotalCount();
  }
}
