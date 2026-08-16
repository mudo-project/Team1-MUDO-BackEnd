package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.google.application.port.GoogleConnectionUserDirectoryPort;
import com.academy.mudogroupware.google.application.port.GoogleConnectionUserInfo;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GoogleConnectionUserDirectoryAdapter implements GoogleConnectionUserDirectoryPort {

    private final UserRepository userRepository;

    /**
     * Consumer: google
     * Purpose: Resolve the historical Google connection administrator name without exposing users entities.
     */
    @Override
    public Optional<GoogleConnectionUserInfo> findByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new GoogleConnectionUserInfo(user.getId(), user.getName()));
    }
}
