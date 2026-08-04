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

    private final UserInfoJpaRepository userInfoJpaRepository;

    @Override
    public ChatMemberInfo getMember(Long userId) {
        UserInfoEntity entity = userInfoJpaRepository.findById(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.MEMBER_NOT_FOUND));
        return toMemberInfo(entity);
    }

    @Override
    public Map<Long, ChatMemberInfo> getMembers(List<Long> userIds) {
        return userInfoJpaRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserInfoEntity::getId, this::toMemberInfo));
    }

    private ChatMemberInfo toMemberInfo(UserInfoEntity entity) {
        return new ChatMemberInfo(entity.getId(), entity.getName(), entity.getAcademyId());
    }
}
