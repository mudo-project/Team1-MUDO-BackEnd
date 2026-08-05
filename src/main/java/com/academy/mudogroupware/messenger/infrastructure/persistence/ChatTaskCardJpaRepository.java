package com.academy.mudogroupware.messenger.infrastructure.persistence;

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
    List<ChatTaskCardEntity> findAllByChatRoomIdOrderByCreatedAtDescIdDesc(Long chatRoomId);

    @Modifying
    @Query(value = "update chat_task_assignee set completed_at = :completedAt "
            + "where card_id = :cardId and user_id = :userId and completed_at is null", nativeQuery = true)
    int markCompleted(@Param("cardId") Long cardId, @Param("userId") Long userId,
                       @Param("completedAt") LocalDateTime completedAt);
}
