package com.academy.mudogroupware.users.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.usecase.UpdateMemberProfileUseCase;
import com.academy.mudogroupware.users.application.usecase.UpdateMyProfileUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileService implements UpdateMyProfileUseCase, UpdateMemberProfileUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public void updateMyProfile(Long userId, String phone, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        userRepository.updateProfile(user.getId(), user.getName(),
                phone != null ? phone : user.getPhone(),
                email != null ? email : user.getEmail(),
                user.getJoinedAt());
    }

    @Override
    public void updateMemberProfile(Long userId, String name, String phone, String email,
                                     LocalDateTime joinedAt, Long roleId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getAccountType() == AccountType.MEMBER)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (roleId != null) {
            roleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new);
            userRepository.changeRole(user.getId(), roleId);
        }

        userRepository.updateProfile(user.getId(),
                name != null ? name : user.getName(),
                phone != null ? phone : user.getPhone(),
                email != null ? email : user.getEmail(),
                joinedAt != null ? joinedAt : user.getJoinedAt());
    }
}
