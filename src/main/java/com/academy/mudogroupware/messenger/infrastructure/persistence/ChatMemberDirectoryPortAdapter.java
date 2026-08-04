package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMemberDirectoryPortAdapter implements ChatMemberDirectoryPort {

    private final ChatMemberInfoJpaRepository chatMemberInfoJpaRepository;

    @Override
    public ChatMemberInfo getMember(Long userId) {
        ChatMemberInfoEntity entity = chatMemberInfoJpaRepository.findById(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.MEMBER_NOT_FOUND));
        return toMemberInfo(entity);
    }

    @Override
    public Map<Long, ChatMemberInfo> getMembers(List<Long> userIds) {
        return chatMemberInfoJpaRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(ChatMemberInfoEntity::getId, this::toMemberInfo));
    }

    private ChatMemberInfo toMemberInfo(ChatMemberInfoEntity entity) {
        return new ChatMemberInfo(entity.getId(), entity.getName(), entity.getAcademyId());
    }
}
