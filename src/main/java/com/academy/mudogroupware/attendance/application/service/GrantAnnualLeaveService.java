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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrantAnnualLeaveService implements GrantAnnualLeaveUseCase {

    private final LeaveGrantEmployeePort leaveGrantEmployeePort;
    private final LeaveGrantRepository leaveGrantRepository;

    @Override
    @Transactional
    public int grantAnnualLeave(LocalDateTime now) {
        log.info("event=attendance_annual_leave_grant_시작 grantDate={}", now.toLocalDate());
        try {
        LocalDate today = now.toLocalDate();
        int grantedCount = 0;
        for (LeaveGrantEmployee employee : leaveGrantEmployeePort.findActiveEmployeesWithJoinedDate()) {
            LocalDate grantDate = currentGrantDate(employee.joinedDate(), today);
            if (grantDate == null) {
                continue;
            }
            if (leaveGrantRepository.saveIfAbsent(LeaveGrant.annual(
                    employee.userId(), grantDate, now))) {
                grantedCount++;
            }
        }
        log.info("event=attendance_annual_leave_grant_완료 grantDate={}, count={}", today, grantedCount);
        return grantedCount;
        } catch (RuntimeException e) { log.warn("event=attendance_annual_leave_grant_실패 grantDate={}, errorType={}", now.toLocalDate(), e.getClass().getSimpleName()); throw e; }
    }

    private LocalDate currentGrantDate(LocalDate joinedDate, LocalDate today) {
        if (joinedDate.isAfter(today)) {
            return null;
        }
        LocalDate anniversary = joinedDate.plusYears(today.getYear() - joinedDate.getYear());
        return anniversary.isAfter(today) ? anniversary.minusYears(1) : anniversary;
    }
}
