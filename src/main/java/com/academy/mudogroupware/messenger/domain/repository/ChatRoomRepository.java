package com.academy.mudogroupware.messenger.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.messenger.domain.model.ChatRoom;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    List<ChatRoom> findAllByMember(Long academyId, Long userId);

    // 요청자 행만 원자적으로 갱신한다(방 전체를 다시 저장하는 방식은 동시에 다른 멤버의 읽음 갱신을 덮어쓸 위험이 있어 대신 사용).
    void markRead(Long chatRoomId, Long userId, LocalDateTime readAt);
}
