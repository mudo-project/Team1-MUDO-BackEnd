package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatTaskCardJpaRepository extends JpaRepository<ChatTaskCardEntity, Long> {

    @EntityGraph(attributePaths = "assignees")
    Optional<ChatTaskCardEntity> findById(Long id);

    @EntityGraph(attributePaths = "assignees")
    List<ChatTaskCardEntity> findAllByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long chatRoomId);

    @Modifying
    @Query(value = "update chat_task_assignee set completed_at = :completedAt "
            + "where card_id = :cardId and user_id = :userId and completed_at is null", nativeQuery = true)
    int markCompleted(@Param("cardId") Long cardId, @Param("userId") Long userId,
                       @Param("completedAt") LocalDateTime completedAt);

    // 담당자 목록 수정 시 유지되는 담당자 row는 건드리지 않기 위해, 전체 재저장 대신 빠진/추가된 담당자만
    // 각각 삭제·삽입한다(동시에 진행 중인 markCompleted와의 유실 방지).
    @Modifying
    @Query(value = "delete from chat_task_assignee where card_id = :cardId and user_id in :userIds",
            nativeQuery = true)
    void deleteAssignees(@Param("cardId") Long cardId, @Param("userIds") List<Long> userIds);

    @Modifying
    @Query(value = "insert into chat_task_assignee (card_id, user_id, completed_at) values (:cardId, :userId, null)",
            nativeQuery = true)
    void insertAssignee(@Param("cardId") Long cardId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "update chat_task_card set content = :content, due_date = :dueDate "
            + "where card_id = :cardId and deleted_at is null", nativeQuery = true)
    int updateContent(@Param("cardId") Long cardId, @Param("content") String content,
                       @Param("dueDate") LocalDate dueDate);

    @Modifying
    @Query(value = "update chat_task_card set deleted_at = :deletedAt "
            + "where card_id = :cardId and deleted_at is null", nativeQuery = true)
    int markDeleted(@Param("cardId") Long cardId, @Param("deletedAt") LocalDateTime deletedAt);
}
