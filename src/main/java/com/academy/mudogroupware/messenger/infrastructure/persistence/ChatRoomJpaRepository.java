package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, Long> {

    @EntityGraph(attributePaths = "members")
    Optional<ChatRoomEntity> findById(Long id);

    @Query("select distinct r from ChatRoomEntity r "
            + "join r.members requester "
            + "join r.members participant "
            + "join fetch r.members "
            + "where r.academyId = :academyId "
            + "and r.type = :type "
            + "and requester.userId = :userId "
            + "and participant.userId = :otherUserId")
    List<ChatRoomEntity> findDirectMessages(@Param("academyId") Long academyId,
                                            @Param("type") ChatRoomType type,
                                            @Param("userId") Long userId,
                                            @Param("otherUserId") Long otherUserId);

    @Query("select distinct r from ChatRoomEntity r join r.members requester join fetch r.members "
            + "where r.academyId = :academyId and requester.userId = :userId")
    List<ChatRoomEntity> findAllByMember(@Param("academyId") Long academyId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "update chat_room_member set last_read_at = :readAt "
            + "where chat_room_id = :chatRoomId and user_id = :userId "
            + "and (last_read_at is null or :readAt > last_read_at)", nativeQuery = true)
    int markRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId,
                 @Param("readAt") LocalDateTime readAt);
}
