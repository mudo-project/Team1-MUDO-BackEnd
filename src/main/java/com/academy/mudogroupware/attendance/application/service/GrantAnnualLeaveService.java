package com.academy.mudogroupware.attendance.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployee;
import com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployeePort;
import com.academy.mudogroupware.attendance.application.usecase.GrantAnnualLeaveUseCase;
import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrantAnnualLeaveService implements GrantAnnualLeaveUseCase {

    private final LeaveGrantEmployeePort leaveGrantEmployeePort;
    private final LeaveGrantRepository leaveGrantRepository;

    @Override
    @Transactional
    public int grantAnnualLeave(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        int grantedCount = 0;
        for (LeaveGrantEmployee employee : leaveGrantEmployeePort.findActiveEmployeesWithJoinedDate()) {
            LocalDate grantDate = currentGrantDate(employee.joinedDate(), today);
            if (grantDate == null || leaveGrantRepository.existsByAcademyIdAndUserIdAndGrantDate(
                    employee.academyId(), employee.userId(), grantDate)) {
                continue;
            }
            leaveGrantRepository.save(LeaveGrant.annual(
                    employee.academyId(), employee.userId(), grantDate, now));
            grantedCount++;
        }
        return grantedCount;
    }

    private LocalDate currentGrantDate(LocalDate joinedDate, LocalDate today) {
        if (joinedDate.isAfter(today)) {
            return null;
        }
        LocalDate anniversary = joinedDate.plusYears(today.getYear() - joinedDate.getYear());
        return anniversary.isAfter(today) ? anniversary.minusYears(1) : anniversary;
    }
}
