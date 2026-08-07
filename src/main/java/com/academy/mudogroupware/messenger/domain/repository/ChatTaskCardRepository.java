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

    // 담당자 완료 행만 원자적으로 갱신한다(카드 전체를 다시 저장하는 방식은 동시 완료 처리 시 유실 위험이 있어 대신 사용).
    void markAssigneeCompleted(Long cardId, Long userId, LocalDateTime completedAt);

    // 삭제된 카드는 갱신하지 않는다(deleted_at is null 조건). 반환값이 false면 이미 삭제된 카드라는 뜻이다.
    boolean updateContent(Long cardId, String content, LocalDate dueDate);

    // addedUserIds만 삽입, removedUserIds만 삭제 — 유지되는 담당자 row는 건드리지 않는다.
    void replaceAssignees(Long cardId, List<Long> addedUserIds, List<Long> removedUserIds);

    void markDeleted(Long cardId, LocalDateTime deletedAt);
}
