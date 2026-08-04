package com.academy.mudogroupware.messenger.infrastructure.persistence;

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
        return chatTaskCardJpaRepository.findAllByChatRoomId(chatRoomId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markAssigneeCompleted(Long cardId, Long userId, LocalDateTime completedAt) {
        chatTaskCardJpaRepository.markCompleted(cardId, userId, completedAt);
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
                .assignees(assignees)
                .build();
    }

    private ChatTaskCard toDomain(ChatTaskCardEntity entity) {
        List<ChatTaskAssignee> assignees = entity.getAssignees().stream()
                .map(assignee -> ChatTaskAssignee.restore(assignee.getUserId(), assignee.getCompletedAt()))
                .toList();

        return ChatTaskCard.restore(entity.getId(), entity.getChatRoomId(), entity.getAssignerUserId(),
                entity.getContent(), entity.getDueDate(), assignees, entity.getCreatedAt());
    }
}
