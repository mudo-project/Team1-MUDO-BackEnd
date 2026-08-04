package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("select distinct r from ChatRoomEntity r join r.members m "
            + "where r.academyId = :academyId and m.userId = :userId")
    List<ChatRoomEntity> findAllByMember(@Param("academyId") Long academyId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "update chat_room_member set last_read_at = :readAt "
            + "where chat_room_id = :chatRoomId and user_id = :userId "
            + "and (last_read_at is null or :readAt > last_read_at)", nativeQuery = true)
    int markRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId,
                 @Param("readAt") LocalDateTime readAt);
}
