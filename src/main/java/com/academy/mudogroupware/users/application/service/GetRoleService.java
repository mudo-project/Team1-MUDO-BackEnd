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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRoleService implements GetRoleUseCase {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public RoleView getRole(Long roleId, Long academyId) {
        log.info("event=role_get_시작 roleId={}, academyId={}", roleId, academyId);
        try {
            Role role = roleRepository.findById(roleId)
                    .filter(r -> r.getAcademyId().equals(academyId))
                    .orElseThrow(RoleNotFoundException::new);
            long memberCount = userRepository.countActiveByRoleIds(Set.of(role.getId()))
                    .getOrDefault(role.getId(), 0L);
            log.info("event=role_get_완료 roleId={}, memberCount={}", roleId, memberCount);
            return new RoleView(role, memberCount);
        } catch (RuntimeException e) {
            log.warn("event=role_get_실패 roleId={}, academyId={}, reason={}", roleId, academyId, e.getMessage());
            throw e;
        }
    }
}
