package com.academy.mudogroupware.messenger.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;

public interface ChatTaskCardRepository {

    // 신규 생성 시에만 사용한다. 생성 이후의 모든 변경(수정/완료/삭제)은 아래의 targeted 메서드로 처리한다
    // (담당자 @ElementCollection을 통째로 다시 쓰면 동시에 진행 중인 다른 targeted 갱신을 유실시킬 수 있어서).
    ChatTaskCard save(ChatTaskCard chatTaskCard);

    Optional<ChatTaskCard> findById(Long id);

    // messenger 메시지 목록조회와 동일한 cursor 페이지네이션. createdAt 내림차순(최신순, id로 동일 시각
    // 타이브레이크)이며 삭제되지 않은 카드만 반환한다. size+1개를 가져와 hasNext 판단은 호출부에서 한다.
    List<ChatTaskCard> findPage(Long chatRoomId, LocalDateTime cursorCreatedAt, Long cursorCardId, int size);

    // 방을 가리지 않고 assignerUserId가 나인 카드(내가 전달한 업무)를 findPage와 동일한 cursor
    // 페이지네이션으로 조회한다.
    List<ChatTaskCard> findSentPage(Long assignerUserId, LocalDateTime cursorCreatedAt, Long cursorCardId, int size);

    // 방을 가리지 않고 담당자 목록에 내가 포함된 카드(내가 받은 업무)를 findPage와 동일한 cursor
    // 페이지네이션으로 조회한다.
    List<ChatTaskCard> findReceivedPage(Long assigneeUserId, LocalDateTime cursorCreatedAt, Long cursorCardId,
                                        int size);

    // 담당자 완료 행만 원자적으로 갱신한다(카드 전체를 다시 저장하는 방식은 동시 완료 처리 시 유실 위험이 있어 대신 사용).
    // 카드가 삭제된 상태면 갱신하지 않는다(deleted_at is null 조건). 반환값이 false면 이미 완료된 상태였거나
    // 이 갱신 시점에 카드가 (동시에) 삭제됐다는 뜻이다 — 어느 쪽인지는 isDeleted()로 다시 확인해야 한다.
    boolean markAssigneeCompleted(Long cardId, Long userId, LocalDateTime completedAt);

    // markAssigneeCompleted가 0건 갱신됐을 때 그 이유(이미 완료됨 vs 카드 삭제됨)를 가리기 위한 조회.
    // 트랜잭션의 스냅샷이 아니라 커밋된 최신 상태를 봐야 하므로 잠금 조회로 구현한다.
    boolean isDeleted(Long cardId);

    // 삭제된 카드는 갱신하지 않는다(deleted_at is null 조건). 반환값이 false면 이미 삭제된 카드라는 뜻이다.
    boolean updateContent(Long cardId, String content, LocalDate dueDate);

    // addedUserIds만 삽입, removedUserIds만 삭제 — 유지되는 담당자 row는 건드리지 않는다.
    void replaceAssignees(Long cardId, List<Long> addedUserIds, List<Long> removedUserIds);

    // 삭제된 카드는 다시 갱신하지 않는다(deleted_at is null 조건). 반환값이 false면 이미 삭제된 카드라는
    // 뜻이다(동시 삭제 요청 중 하나가 먼저 커밋한 경우 포함).
    boolean markDeleted(Long cardId, LocalDateTime deletedAt);
}
