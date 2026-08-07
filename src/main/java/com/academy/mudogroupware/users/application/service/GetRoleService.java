package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.GetRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRoleService implements GetRoleUseCase {

    private final RoleRepository roleRepository;

    @Override
    public Role getRole(Long roleId, Long academyId) {
        return roleRepository.findById(roleId)
                .filter(role -> role.getAcademyId().equals(academyId))
                .orElseThrow(RoleNotFoundException::new);
    }
}
