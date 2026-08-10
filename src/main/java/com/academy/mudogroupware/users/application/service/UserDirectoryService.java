package com.academy.mudogroupware.users.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.UserDirectoryUseCase;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserDirectoryService implements UserDirectoryUseCase {

    private final UserRepository userRepository;

    @Override
    public Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds) {
        log.info("event=user_directory_find_active_ids_시작 academyId={}, requestedCount={}", academyId,
                userIds.size());
        Set<Long> result = userRepository.findActiveUserIds(academyId, userIds);
        log.info("event=user_directory_find_active_ids_완료 academyId={}, activeCount={}", academyId, result.size());
        return result;
    }
}
