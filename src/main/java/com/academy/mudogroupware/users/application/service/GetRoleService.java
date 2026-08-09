package com.academy.mudogroupware.users.application.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.query.RoleView;
import com.academy.mudogroupware.users.application.usecase.GetRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRoleService implements GetRoleUseCase {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public RoleView getRole(Long roleId, Long academyId) {
        Role role = roleRepository.findById(roleId)
                .filter(r -> r.getAcademyId().equals(academyId))
                .orElseThrow(RoleNotFoundException::new);
        long memberCount = userRepository.countActiveByRoleIds(Set.of(role.getId())).getOrDefault(role.getId(), 0L);
        return new RoleView(role, memberCount);
    }
}
