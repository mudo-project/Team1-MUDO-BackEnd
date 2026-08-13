package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatTaskCardJpaRepository extends JpaRepository<ChatTaskCardEntity, Long> {

    @EntityGraph(attributePaths = "assignees")
    Optional<ChatTaskCardEntity> findById(Long id);

    // messenger 메시지 목록조회와 동일한 cursor 페이지네이션 패턴. 페이지네이션 없이 방의 카드를 전부
    // 반환하던 이전 버전은 카드가 쌓일수록 응답이 무한히 커져(부하테스트로 1,000건에 374KB 확인, 2026-08-07)
    // memo 목록조회와 같은 문제가 있었다.
    @EntityGraph(attributePaths = "assignees")
    @Query("select c from ChatTaskCardEntity c where c.chatRoomId = :chatRoomId and c.deletedAt is null "
            + "and (:cursorCreatedAt is null "
            + "or c.createdAt < :cursorCreatedAt "
            + "or (c.createdAt = :cursorCreatedAt and c.id < :cursorCardId)) "
            + "order by c.createdAt desc, c.id desc")
    List<ChatTaskCardEntity> findPage(@Param("chatRoomId") Long chatRoomId,
                                       @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                       @Param("cursorCardId") Long cursorCardId,
                                       Pageable pageable);

    // chat_task_card.deleted_at도 함께 확인해, 이 UPDATE 시점에 카드가 이미(동시에) 삭제됐다면
    // 완료 처리를 반영하지 않는다(반환값 0으로 호출측이 감지). 서브쿼리 형태라 H2(테스트)/MySQL(운영)
    // 양쪽에서 동일하게 동작한다(MySQL 전용 멀티테이블 UPDATE...JOIN 문법은 H2가 지원하지 않는다).
    @Modifying
    @Query(value = "update chat_task_assignee set completed_at = :completedAt "
            + "where card_id = :cardId and user_id = :userId and completed_at is null "
            + "and exists (select 1 from chat_task_card where card_id = :cardId and deleted_at is null)",
            nativeQuery = true)
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

    // 완료된 담당자가 있는지도 함께 확인해, 이 UPDATE 시점에 담당자가 (동시에) 완료 처리됐다면
    // 삭제를 반영하지 않는다(반환값 0으로 호출측이 감지). NOT EXISTS 서브쿼리는 표준 SQL이라
    // H2(테스트)/MySQL(운영) 양쪽에서 동일하게 동작한다.
    @Modifying
    @Query(value = "update chat_task_card set deleted_at = :deletedAt "
            + "where card_id = :cardId and deleted_at is null "
            + "and not exists (select 1 from chat_task_assignee "
            + "where card_id = :cardId and completed_at is not null)", nativeQuery = true)
    int markDeleted(@Param("cardId") Long cardId, @Param("deletedAt") LocalDateTime deletedAt);

    // 잠금 조회(FOR UPDATE)라 트랜잭션의 스냅샷이 아니라 커밋된 최신 deleted_at을 본다. H2/MySQL 둘 다
    // 지원하는 표준 문법이라 markCompleted 때와 달리 별도 분기가 필요 없다.
    @Query(value = "select deleted_at from chat_task_card where card_id = :cardId for update", nativeQuery = true)
    LocalDateTime findDeletedAtForUpdate(@Param("cardId") Long cardId);
}
