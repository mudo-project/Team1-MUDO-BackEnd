package com.academy.mudogroupware.messenger.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;

public interface ChatTaskCardRepository {

    ChatTaskCard save(ChatTaskCard chatTaskCard);

    Optional<ChatTaskCard> findById(Long id);

    List<ChatTaskCard> findAllByChatRoomId(Long chatRoomId);
}
