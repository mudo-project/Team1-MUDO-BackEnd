package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.usecase.ChangeUserStatusUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserStatusService implements ChangeUserStatusUseCase {

    private final UserRepository userRepository;

    @Override
    public void changeStatus(Long academyId, Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getAcademyId().equals(academyId))
                .filter(u -> u.getAccountType() == AccountType.MEMBER)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        userRepository.changeStatus(user.getId(), status);
    }
}
