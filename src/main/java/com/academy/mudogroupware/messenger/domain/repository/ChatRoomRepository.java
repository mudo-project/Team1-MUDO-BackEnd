package com.academy.mudogroupware.messenger.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.messenger.domain.model.ChatRoom;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    List<ChatRoom> findAllByMember(Long academyId, Long userId);
}
