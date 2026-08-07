package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.EmploymentSummaryPort;
import com.academy.mudogroupware.attendance.application.usecase.GetMyEmploymentSummaryUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEmploymentSummaryQueryService implements GetMyEmploymentSummaryUseCase {

    private final EmploymentSummaryPort employmentSummaryPort;
    private final Clock clock;

    @Override
    public MyEmploymentSummaryView getSummary(Long userId, Long academyId) {
        LocalDate hireDate = employmentSummaryPort.findByUserIdAndAcademyId(userId, academyId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.EMPLOYMENT_INFO_NOT_FOUND))
                .hireDate();
        long tenureDays = Math.max(0, ChronoUnit.DAYS.between(hireDate, LocalDate.now(clock)));
        return new MyEmploymentSummaryView(hireDate, tenureDays);
    }
}
