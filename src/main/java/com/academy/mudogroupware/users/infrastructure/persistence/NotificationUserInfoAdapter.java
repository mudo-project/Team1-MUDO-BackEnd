package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.notification.application.port.NotificationUserInfoPort;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationUserInfoAdapter implements NotificationUserInfoPort {

    private final UserRepository userRepository;

    /**
     * Consumer: notification
     *
     * <p>Purpose: 멘션 알림 문구에 들어갈 행위자(actor) 이름 조회
     */
    @Override
    public List<NotificationUserInfo> findUserInfo(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(user -> new NotificationUserInfo(user.getId(), user.getName()))
                .toList();
    }
}
