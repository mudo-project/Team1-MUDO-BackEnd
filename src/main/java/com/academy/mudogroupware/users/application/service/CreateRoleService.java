package com.academy.mudogroupware.users.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.CreateRoleCommand;
import com.academy.mudogroupware.users.application.usecase.CreateRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateRoleService implements CreateRoleUseCase {

    private final RoleRepository roleRepository;
    private final Clock clock;

    @Override
    public Long createRole(CreateRoleCommand command) {
        log.info("event=role_create_시작 name={}", command.name());
        try {
            if (roleRepository.existsByName(command.name())) {
                throw new RoleNameDuplicateException();
            }

            Role role = Role.create(command.name(), command.description(), command.color(),
                    LocalDateTime.now(clock));
            Long roleId = roleRepository.save(role).getId();
            log.info("event=role_create_완료 roleId={}", roleId);
            return roleId;
        } catch (RuntimeException e) {
            log.warn("event=role_create_실패 name={}, reason={}", command.name(),
                    e.getMessage(), e);
            throw e;
        }
    }
}
