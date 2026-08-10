package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployee;
import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;

@ExtendWith(MockitoExtension.class)
class GrantAnnualLeaveServiceTest {

    @Mock
    private com.academy.mudogroupware.attendance.application.port.LeaveGrantEmployeePort employeePort;
    @Mock
    private LeaveGrantRepository leaveGrantRepository;

    @Test
    void grantsCurrentAnnualPeriodWhenMissing() {
        GrantAnnualLeaveService service = new GrantAnnualLeaveService(employeePort, leaveGrantRepository);
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 0, 5);
        when(employeePort.findActiveEmployeesWithJoinedDate())
                .thenReturn(List.of(new LeaveGrantEmployee(2L, LocalDate.of(2024, 10, 10))));
        when(leaveGrantRepository.existsByUserIdAndGrantDate(
                2L, LocalDate.of(2025, 10, 10))).thenReturn(false);

        int count = service.grantAnnualLeave(now);

        assertEquals(1, count);
        ArgumentCaptor<LeaveGrant> captor = ArgumentCaptor.forClass(LeaveGrant.class);
        verify(leaveGrantRepository).save(captor.capture());
        assertEquals(LocalDate.of(2025, 10, 10), captor.getValue().getGrantDate());
        assertEquals(LocalDate.of(2026, 10, 9), captor.getValue().getExpirationDate());
        assertEquals(15, captor.getValue().getGrantedDays());
    }

    @Test
    void skipsExistingCurrentPeriod() {
        GrantAnnualLeaveService service = new GrantAnnualLeaveService(employeePort, leaveGrantRepository);
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 0, 5);
        when(employeePort.findActiveEmployeesWithJoinedDate())
                .thenReturn(List.of(new LeaveGrantEmployee(2L, LocalDate.of(2024, 8, 6))));
        when(leaveGrantRepository.existsByUserIdAndGrantDate(
                2L, LocalDate.of(2026, 8, 6))).thenReturn(true);

        int count = service.grantAnnualLeave(now);

        assertEquals(0, count);
        verify(leaveGrantRepository, never()).save(any());
    }
}
