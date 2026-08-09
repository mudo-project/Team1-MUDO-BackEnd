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

@Service
@RequiredArgsConstructor
@Transactional
public class CreateRoleService implements CreateRoleUseCase {

    private final RoleRepository roleRepository;
    private final Clock clock;

    @Override
    public Long createRole(CreateRoleCommand command) {
        if (roleRepository.existsByAcademyIdAndName(command.academyId(), command.name())) {
            throw new RoleNameDuplicateException();
        }

        Role role = Role.create(command.academyId(), command.name(), command.description(), command.color(),
                LocalDateTime.now(clock));
        return roleRepository.save(role).getId();
    }
}
