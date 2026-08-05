package com.academy.mudogroupware.messenger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatMessageJpaRepositoryTest {

    @Autowired
    private ChatMessageJpaRepository chatMessageJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unreadCountExcludesMessagesSentByRequester() {
        jdbcTemplate.update("""
                insert into chat_room (chat_room_id, academy_id, type, created_by, created_at)
                values (1, 10, 'DM', 1, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into chat_room_member (chat_room_id, user_id, last_read_at)
                values (1, 1, null)
                """);
        jdbcTemplate.update("""
                insert into chat_message (
                    message_id, chat_room_id, sender_user_id, message_type, content, created_at
                ) values (1, 1, 1, 'TEXT', 'mine', current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into chat_message (
                    message_id, chat_room_id, sender_user_id, message_type, content, created_at
                ) values (2, 1, 2, 'TEXT', 'theirs', current_timestamp)
                """);

        Map<Long, Long> unreadCounts = chatMessageJpaRepository.countUnreadByRequester(1L, List.of(1L)).stream()
                .collect(Collectors.toMap(ChatRoomUnreadCountProjection::getChatRoomId,
                        ChatRoomUnreadCountProjection::getUnreadCount));

        assertThat(unreadCounts).containsEntry(1L, 1L);
    }

    @Test
    void countsUnreadMembersForEachMessage() {
        jdbcTemplate.update("""
                insert into chat_room (chat_room_id, academy_id, type, created_by, created_at)
                values (1, 10, 'GROUP', 1, timestamp '2026-08-05 09:00:00')
                """);
        jdbcTemplate.update("""
                insert into chat_room_member (chat_room_id, user_id, last_read_at)
                values (1, 1, timestamp '2026-08-05 10:20:00')
                """);
        jdbcTemplate.update("""
                insert into chat_room_member (chat_room_id, user_id, last_read_at)
                values (1, 2, timestamp '2026-08-05 10:05:00')
                """);
        jdbcTemplate.update("""
                insert into chat_room_member (chat_room_id, user_id, last_read_at)
                values (1, 3, null)
                """);
        jdbcTemplate.update("""
                insert into chat_message (
                    message_id, chat_room_id, sender_user_id, message_type, content, created_at
                ) values (1, 1, 1, 'TEXT', 'first', timestamp '2026-08-05 10:00:00')
                """);
        jdbcTemplate.update("""
                insert into chat_message (
                    message_id, chat_room_id, sender_user_id, message_type, content, created_at
                ) values (2, 1, 1, 'TEXT', 'second', timestamp '2026-08-05 10:10:00')
                """);

        Map<Long, Long> unreadCounts = chatMessageJpaRepository.countUnreadByMessageIds(1L, List.of(1L, 2L)).stream()
                .collect(Collectors.toMap(MessageUnreadCountProjection::getMessageId,
                        MessageUnreadCountProjection::getUnreadCount));

        assertThat(unreadCounts).containsEntry(1L, 1L);
        assertThat(unreadCounts).containsEntry(2L, 2L);
    }
}
