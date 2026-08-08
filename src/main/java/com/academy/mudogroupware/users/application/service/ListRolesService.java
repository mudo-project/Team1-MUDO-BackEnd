package com.academy.mudogroupware.users.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.query.RoleView;
import com.academy.mudogroupware.users.application.usecase.ListRolesUseCase;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListRolesService implements ListRolesUseCase {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public List<RoleView> listRoles(Long academyId) {
        List<Role> roles = roleRepository.findAllByAcademyId(academyId);
        Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        Map<Long, Long> memberCounts = userRepository.countActiveByRoleIds(roleIds);
        return roles.stream()
                .map(role -> new RoleView(role, memberCounts.getOrDefault(role.getId(), 0L)))
                .toList();
    }
}
