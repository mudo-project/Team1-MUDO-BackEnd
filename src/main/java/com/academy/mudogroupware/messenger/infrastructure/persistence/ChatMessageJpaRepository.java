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

    @Query(value = "select crm.chat_room_id as chatRoomId, count(cm.message_id) as unreadCount "
            + "from chat_room_member crm "
            + "left join chat_message cm on cm.chat_room_id = crm.chat_room_id "
            + "and (crm.last_read_at is null or cm.created_at > crm.last_read_at) "
            + "and cm.sender_user_id <> :userId "
            + "where crm.user_id = :userId and crm.chat_room_id in :chatRoomIds "
            + "group by crm.chat_room_id", nativeQuery = true)
    List<ChatRoomUnreadCountProjection> countUnreadByRequester(@Param("userId") Long userId,
                                                                @Param("chatRoomIds") List<Long> chatRoomIds);

    @Query(value = "select cm.message_id as messageId, count(crm.user_id) as unreadCount "
            + "from chat_message cm "
            + "left join chat_room_member crm on crm.chat_room_id = cm.chat_room_id "
            + "and crm.user_id <> cm.sender_user_id "
            + "and (crm.last_read_at is null or crm.last_read_at < cm.created_at) "
            + "where cm.chat_room_id = :chatRoomId and cm.message_id in :messageIds "
            + "group by cm.message_id", nativeQuery = true)
    List<MessageUnreadCountProjection> countUnreadByMessageIds(@Param("chatRoomId") Long chatRoomId,
                                                               @Param("messageIds") List<Long> messageIds);

    @Query("select m from ChatMessageEntity m where m.chatRoomId in :chatRoomIds "
            + "and m.createdAt = (select max(m2.createdAt) from ChatMessageEntity m2 "
            + "where m2.chatRoomId = m.chatRoomId)")
    List<ChatMessageEntity> findLatestByChatRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);
}
