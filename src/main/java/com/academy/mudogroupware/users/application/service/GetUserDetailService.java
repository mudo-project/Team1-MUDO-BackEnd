package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.result.UserDetailResult;
import com.academy.mudogroupware.users.application.usecase.GetMemberDetailUseCase;
import com.academy.mudogroupware.users.application.usecase.GetMyProfileUseCase;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserDetailService implements GetMyProfileUseCase, GetMemberDetailUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetailResult getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return toResult(user);
    }

    @Override
    public UserDetailResult getMemberDetail(Long academyId, Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getAcademyId().equals(academyId))
                .filter(u -> u.getAccountType() == AccountType.MEMBER)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return toResult(user);
    }

    private UserDetailResult toResult(User user) {
        String roleName = user.getRoleId() == null
                ? null
                : roleRepository.findById(user.getRoleId()).map(Role::getName).orElse(null);
        return new UserDetailResult(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRoleId(), roleName, user.getJoinedAt(), user.getStatus());
    }
}
