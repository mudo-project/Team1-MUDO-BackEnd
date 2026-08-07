package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort;
import com.academy.mudogroupware.users.domain.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttendanceCorrectionRequesterAdapter implements AttendanceCorrectionRequesterPort {

    private final com.academy.mudogroupware.users.domain.repository.UserRepository userRepository;
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public Map<Long, Requester> findByAcademyIdAndUserIds(Long academyId, Set<Long> userIds) {
        Map<Long, String> roleNames = roleJpaRepository.findAllById(
                        userRepository.findAllById(userIds).stream()
                                .map(User::getRoleId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getName));
        return userRepository.findAllById(userIds).stream()
                .filter(user -> academyId.equals(user.getAcademyId()))
                .collect(Collectors.toMap(User::getId,
                        user -> new Requester(user.getId(), user.getName(), roleNames.get(user.getRoleId()))));
    }
}
