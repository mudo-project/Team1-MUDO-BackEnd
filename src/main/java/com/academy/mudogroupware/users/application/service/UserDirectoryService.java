package com.academy.mudogroupware.users.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.UserDirectoryUseCase;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserDirectoryService implements UserDirectoryUseCase {

    private final UserRepository userRepository;

    @Override
    public Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds) {
        return userRepository.findActiveUserIds(academyId, userIds);
    }
}
