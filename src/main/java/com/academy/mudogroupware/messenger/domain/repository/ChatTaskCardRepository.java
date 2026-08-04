package com.academy.mudogroupware.messenger.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;

public interface ChatTaskCardRepository {

    ChatTaskCard save(ChatTaskCard chatTaskCard);

    Optional<ChatTaskCard> findById(Long id);

    List<ChatTaskCard> findAllByChatRoomId(Long chatRoomId);

    // 담당자 완료 행만 원자적으로 갱신한다(카드 전체를 다시 저장하는 방식은 동시 완료 처리 시 유실 위험이 있어 대신 사용).
    void markAssigneeCompleted(Long cardId, Long userId, LocalDateTime completedAt);
}
