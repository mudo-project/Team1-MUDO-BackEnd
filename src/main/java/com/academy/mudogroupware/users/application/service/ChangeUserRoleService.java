package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.command.ChangeUserRoleCommand;
import com.academy.mudogroupware.users.application.usecase.ChangeUserRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserRoleService implements ChangeUserRoleUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public void changeRole(ChangeUserRoleCommand command) {
        log.info("event=user_role_change_시작 userId={}, roleId={}, academyId={}", command.userId(),
                command.roleId(), command.academyId());
        try {
            User user = userRepository.findById(command.userId())
                    .filter(u -> u.getAcademyId().equals(command.academyId()))
                    .filter(u -> u.getAccountType() == AccountType.MEMBER)
                    .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

            Role role = roleRepository.findById(command.roleId())
                    .filter(r -> r.getAcademyId().equals(command.academyId()))
                    .orElseThrow(RoleNotFoundException::new);

            userRepository.changeRole(user.getId(), role.getId());
            log.info("event=user_role_change_완료 userId={}, roleId={}", user.getId(), role.getId());
        } catch (RuntimeException e) {
            log.warn("event=user_role_change_실패 userId={}, roleId={}, academyId={}, reason={}", command.userId(),
                    command.roleId(), command.academyId(), e.getMessage(), e);
            throw e;
        }
    }
}
