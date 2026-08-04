package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatTaskCardJpaRepository extends JpaRepository<ChatTaskCardEntity, Long> {

    List<ChatTaskCardEntity> findAllByChatRoomId(Long chatRoomId);
}
