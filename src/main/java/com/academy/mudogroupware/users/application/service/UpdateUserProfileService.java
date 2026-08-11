package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.UpdateMyProfileUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileService implements UpdateMyProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public void updateMyProfile(Long userId, String phone, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        userRepository.updateProfile(user.getId(), user.getName(),
                phone != null ? phone : user.getPhone(),
                email != null ? email : user.getEmail(),
                user.getJoinedAt());
    }
}
