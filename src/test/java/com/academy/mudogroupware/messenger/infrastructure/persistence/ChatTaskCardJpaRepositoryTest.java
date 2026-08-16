package com.academy.mudogroupware.messenger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatTaskCardJpaRepositoryTest {

    @Autowired
    private ChatTaskCardJpaRepository chatTaskCardJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertChatRoom(long chatRoomId) {
        jdbcTemplate.update("""
                insert into chat_room (chat_room_id, type, created_by, created_at)
                values (?, 'GROUP', 1, current_timestamp)
                """, chatRoomId);
    }

    private void insertTaskCard(long cardId, long chatRoomId) {
        jdbcTemplate.update("""
                insert into chat_task_card (card_id, chat_room_id, assigner_user_id, content, due_date, created_at)
                values (?, ?, 1, 'content', '2026-08-10', current_timestamp)
                """, cardId, chatRoomId);
    }

    private void insertAssignee(long cardId, long userId, LocalDateTime completedAt) {
        jdbcTemplate.update("""
                insert into chat_task_assignee (card_id, user_id, completed_at)
                values (?, ?, ?)
                """, cardId, userId, completedAt);
    }

    private LocalDateTime completedAtOf(long cardId, long userId) {
        return jdbcTemplate.queryForObject(
                "select completed_at from chat_task_assignee where card_id = ? and user_id = ?",
                LocalDateTime.class, cardId, userId);
    }

    @Test
    void markCompletedSetsCompletedAtOnlyOnce() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        insertAssignee(7L, 3L, null);

        int firstUpdated = chatTaskCardJpaRepository.markCompleted(7L, 3L, LocalDateTime.of(2026, 8, 6, 9, 30));
        int secondUpdated = chatTaskCardJpaRepository.markCompleted(7L, 3L, LocalDateTime.of(2026, 8, 6, 10, 0));

        assertThat(firstUpdated).isEqualTo(1);
        assertThat(secondUpdated).isEqualTo(0);
        assertThat(completedAtOf(7L, 3L)).isEqualTo(LocalDateTime.of(2026, 8, 6, 9, 30));
    }

    @Test
    void markCompletedSkipsWhenCardAlreadyDeleted() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        insertAssignee(7L, 3L, null);
        chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 9, 0));

        int updated = chatTaskCardJpaRepository.markCompleted(7L, 3L, LocalDateTime.of(2026, 8, 6, 9, 30));

        assertThat(updated).isEqualTo(0);
        assertThat(completedAtOf(7L, 3L)).isNull();
    }

    @Test
    void deleteAssigneesRemovesOnlySpecifiedUsers() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        insertAssignee(7L, 3L, null);
        insertAssignee(7L, 4L, null);

        chatTaskCardJpaRepository.deleteAssignees(7L, List.of(3L));

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from chat_task_assignee where card_id = ?", Integer.class, 7L);
        assertThat(remaining).isEqualTo(1);
        assertThat(completedAtOf(7L, 4L)).isNull();
    }

    @Test
    void insertAssigneeAddsRowWithNullCompletedAt() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);

        chatTaskCardJpaRepository.insertAssignee(7L, 5L);

        assertThat(completedAtOf(7L, 5L)).isNull();
    }

    @Test
    void updateContentSkipsWhenAlreadyDeleted() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 15, 0));

        int updated = chatTaskCardJpaRepository.updateContent(7L, "변경된 내용", LocalDate.of(2026, 8, 20));

        assertThat(updated).isEqualTo(0);
        String content = jdbcTemplate.queryForObject(
                "select content from chat_task_card where card_id = ?", String.class, 7L);
        assertThat(content).isEqualTo("content");
    }

    @Test
    void markDeletedIsIdempotent() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);

        int firstDeleted = chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 15, 0));
        int secondDeleted = chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 16, 0));

        assertThat(firstDeleted).isEqualTo(1);
        assertThat(secondDeleted).isEqualTo(0);
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                "select deleted_at from chat_task_card where card_id = ?", LocalDateTime.class, 7L);
        assertThat(deletedAt).isEqualTo(LocalDateTime.of(2026, 8, 6, 15, 0));
    }

    @Test
    void markDeletedSkipsWhenAssigneeAlreadyCompleted() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        insertAssignee(7L, 3L, LocalDateTime.of(2026, 8, 6, 9, 30));

        int deleted = chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 15, 0));

        assertThat(deleted).isEqualTo(0);
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                "select deleted_at from chat_task_card where card_id = ?", LocalDateTime.class, 7L);
        assertThat(deletedAt).isNull();
    }

    @Test
    void findDeletedAtForUpdateReturnsNullWhenNotDeleted() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);

        assertThat(chatTaskCardJpaRepository.findDeletedAtForUpdate(7L)).isNull();
    }

    @Test
    void findDeletedAtForUpdateReturnsTimestampWhenDeleted() {
        insertChatRoom(1L);
        insertTaskCard(7L, 1L);
        chatTaskCardJpaRepository.markDeleted(7L, LocalDateTime.of(2026, 8, 6, 15, 0));

        assertThat(chatTaskCardJpaRepository.findDeletedAtForUpdate(7L))
                .isEqualTo(LocalDateTime.of(2026, 8, 6, 15, 0));
    }

    private void insertTaskCard(long cardId, long chatRoomId, long assignerUserId, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                insert into chat_task_card (card_id, chat_room_id, assigner_user_id, content, due_date, created_at)
                values (?, ?, ?, 'content', '2026-08-10', ?)
                """, cardId, chatRoomId, assignerUserId, createdAt);
    }

    @Test
    void findSentPageReturnsOnlyCardsAssignedByGivenUser() {
        insertChatRoom(1L);
        insertChatRoom(2L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 10, 0);
        insertTaskCard(7L, 1L, 1L, createdAt);
        insertAssignee(7L, 3L, null);
        insertTaskCard(8L, 2L, 9L, createdAt.plusMinutes(1));
        insertAssignee(8L, 3L, null);

        List<ChatTaskCardEntity> result = chatTaskCardJpaRepository.findSentPage(1L, null, null,
                PageRequest.of(0, 20));

        assertThat(result).extracting(ChatTaskCardEntity::getId).containsExactly(7L);
    }

    @Test
    void findReceivedPageReturnsCardsWhereUserIsAssigneeWithoutDuplicates() {
        insertChatRoom(1L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 10, 0);
        insertTaskCard(7L, 1L, 1L, createdAt);
        insertAssignee(7L, 3L, null);
        insertAssignee(7L, 4L, null);
        insertTaskCard(8L, 1L, 1L, createdAt.plusMinutes(1));
        insertAssignee(8L, 9L, null);

        List<ChatTaskCardEntity> result = chatTaskCardJpaRepository.findReceivedPage(3L, null, null,
                PageRequest.of(0, 20));

        assertThat(result).extracting(ChatTaskCardEntity::getId).containsExactly(7L);
        assertThat(result.get(0).getAssignees()).hasSize(2);
    }
}
