package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {

    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomEntity entity = toEntity(chatRoom);
        return toDomain(chatRoomJpaRepository.save(entity));
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ChatRoom> findDirectMessage(Long userId, Long otherUserId) {
        return chatRoomJpaRepository.findDirectMessages(ChatRoomType.DM, userId, otherUserId).stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<ChatRoom> findAllByMember(Long userId) {
        return chatRoomJpaRepository.findAllByMember(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markRead(Long chatRoomId, Long userId, LocalDateTime readAt) {
        chatRoomJpaRepository.markRead(chatRoomId, userId, readAt);
    }

    private ChatRoomEntity toEntity(ChatRoom chatRoom) {
        List<ChatRoomMemberEmbeddable> members = chatRoom.getMembers().stream()
                .map(member -> ChatRoomMemberEmbeddable.builder()
                        .userId(member.getUserId())
                        .lastReadAt(member.getLastReadAt())
                        .build())
                .toList();

        return ChatRoomEntity.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .createdBy(chatRoom.getCreatedBy())
                .createdAt(chatRoom.getCreatedAt())
                .members(members)
                .build();
    }

    private ChatRoom toDomain(ChatRoomEntity entity) {
        List<ChatRoomMember> members = entity.getMembers().stream()
                .map(member -> ChatRoomMember.restore(member.getUserId(), member.getLastReadAt()))
                .toList();

        return ChatRoom.restore(entity.getId(), entity.getName(), entity.getType(),
                entity.getCreatedBy(), members, entity.getCreatedAt());
    }
}
