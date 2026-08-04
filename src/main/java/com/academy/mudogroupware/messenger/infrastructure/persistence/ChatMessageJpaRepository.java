package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("select m from ChatMessageEntity m where m.chatRoomId = :chatRoomId "
            + "and (:cursorCreatedAt is null "
            + "or m.createdAt < :cursorCreatedAt "
            + "or (m.createdAt = :cursorCreatedAt and m.id < :cursorMessageId)) "
            + "order by m.createdAt desc, m.id desc")
    List<ChatMessageEntity> findPage(@Param("chatRoomId") Long chatRoomId,
                                      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                      @Param("cursorMessageId") Long cursorMessageId,
                                      Pageable pageable);

    @Query("select count(m) from ChatMessageEntity m where m.chatRoomId = :chatRoomId "
            + "and (:after is null or m.createdAt > :after)")
    long countUnread(@Param("chatRoomId") Long chatRoomId, @Param("after") LocalDateTime after);

    @Query("select m from ChatMessageEntity m where m.chatRoomId in :chatRoomIds "
            + "and m.createdAt = (select max(m2.createdAt) from ChatMessageEntity m2 "
            + "where m2.chatRoomId = m.chatRoomId)")
    List<ChatMessageEntity> findLatestByChatRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);
}
