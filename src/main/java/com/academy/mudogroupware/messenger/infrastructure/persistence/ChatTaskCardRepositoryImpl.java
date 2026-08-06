package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatTaskCardRepositoryImpl implements ChatTaskCardRepository {

    private final ChatTaskCardJpaRepository chatTaskCardJpaRepository;

    @Override
    public ChatTaskCard save(ChatTaskCard chatTaskCard) {
        ChatTaskCardEntity entity = toEntity(chatTaskCard);
        return toDomain(chatTaskCardJpaRepository.save(entity));
    }

    @Override
    public Optional<ChatTaskCard> findById(Long id) {
        return chatTaskCardJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ChatTaskCard> findAllByChatRoomId(Long chatRoomId) {
        return chatTaskCardJpaRepository.findAllByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(chatRoomId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markAssigneeCompleted(Long cardId, Long userId, LocalDateTime completedAt) {
        chatTaskCardJpaRepository.markCompleted(cardId, userId, completedAt);
    }

    @Override
    public void updateContent(Long cardId, String content, LocalDate dueDate) {
        chatTaskCardJpaRepository.updateContent(cardId, content, dueDate);
    }

    @Override
    public void replaceAssignees(Long cardId, List<Long> addedUserIds, List<Long> removedUserIds) {
        if (!removedUserIds.isEmpty()) {
            chatTaskCardJpaRepository.deleteAssignees(cardId, removedUserIds);
        }
        addedUserIds.forEach(userId -> chatTaskCardJpaRepository.insertAssignee(cardId, userId));
    }

    @Override
    public void markDeleted(Long cardId, LocalDateTime deletedAt) {
        chatTaskCardJpaRepository.markDeleted(cardId, deletedAt);
    }

    private ChatTaskCardEntity toEntity(ChatTaskCard chatTaskCard) {
        List<ChatTaskAssigneeEmbeddable> assignees = chatTaskCard.getAssignees().stream()
                .map(assignee -> ChatTaskAssigneeEmbeddable.builder()
                        .userId(assignee.getUserId())
                        .completedAt(assignee.getCompletedAt())
                        .build())
                .toList();

        return ChatTaskCardEntity.builder()
                .id(chatTaskCard.getId())
                .chatRoomId(chatTaskCard.getChatRoomId())
                .assignerUserId(chatTaskCard.getAssignerUserId())
                .content(chatTaskCard.getContent())
                .dueDate(chatTaskCard.getDueDate())
                .createdAt(chatTaskCard.getCreatedAt())
                .deletedAt(chatTaskCard.getDeletedAt())
                .assignees(assignees)
                .build();
    }

    private ChatTaskCard toDomain(ChatTaskCardEntity entity) {
        List<ChatTaskAssignee> assignees = entity.getAssignees().stream()
                .map(assignee -> ChatTaskAssignee.restore(assignee.getUserId(), assignee.getCompletedAt()))
                .toList();

        return ChatTaskCard.restore(entity.getId(), entity.getChatRoomId(), entity.getAssignerUserId(),
                entity.getContent(), entity.getDueDate(), assignees, entity.getCreatedAt(), entity.getDeletedAt());
    }
}
