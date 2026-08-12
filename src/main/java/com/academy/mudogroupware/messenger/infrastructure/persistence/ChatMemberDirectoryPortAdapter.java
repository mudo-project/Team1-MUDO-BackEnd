package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.users.application.usecase.UserDirectoryUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMemberDirectoryPortAdapter implements ChatMemberDirectoryPort {

    private final ChatMemberInfoJpaRepository chatMemberInfoJpaRepository;
    private final UserDirectoryUseCase userDirectoryUseCase;

    @Override
    public ChatMemberInfo getMember(Long userId) {
        ChatMemberInfoEntity entity = chatMemberInfoJpaRepository.findById(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.MEMBER_NOT_FOUND));
        if (!isActive(entity)) {
            throw new MessengerException(MessengerErrorCode.MEMBER_NOT_FOUND);
        }
        return toMemberInfo(entity);
    }

    @Override
    public Map<Long, ChatMemberInfo> getMembers(List<Long> userIds) {
        List<ChatMemberInfoEntity> members = chatMemberInfoJpaRepository.findAllById(userIds);
        Set<Long> activeUserIds = findActiveUserIds(members);
        return members.stream()
                .filter(member -> activeUserIds.contains(member.getId()))
                .collect(Collectors.toMap(ChatMemberInfoEntity::getId, this::toMemberInfo));
    }

    private boolean isActive(ChatMemberInfoEntity entity) {
        return userDirectoryUseCase.findActiveUserIds(null, Set.of(entity.getId()))
                .contains(entity.getId());
    }

    private Set<Long> findActiveUserIds(List<ChatMemberInfoEntity> members) {
        Set<Long> memberIds = members.stream().map(ChatMemberInfoEntity::getId).collect(Collectors.toSet());
        return userDirectoryUseCase.findActiveUserIds(null, memberIds);
    }

    private ChatMemberInfo toMemberInfo(ChatMemberInfoEntity entity) {
        return new ChatMemberInfo(entity.getId(), entity.getName());
    }
}
