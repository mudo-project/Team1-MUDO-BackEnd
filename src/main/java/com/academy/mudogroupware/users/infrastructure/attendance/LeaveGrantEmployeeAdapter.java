package com.academy.mudogroupware.users.infrastructure.attendance;

import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployee;
import com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployeePort;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.infrastructure.persistence.UserJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeaveGrantEmployeeAdapter implements LeaveGrantEmployeePort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public List<LeaveGrantEmployee> findActiveEmployeesWithJoinedDate() {
        return userJpaRepository.findAllByStatusAndJoinedAtIsNotNull(UserStatus.ACTIVE).stream()
                .map(user -> new LeaveGrantEmployee(
                        user.getId(), user.getAcademyId(), user.getJoinedAt().toLocalDate()))
                .toList();
    }
}
