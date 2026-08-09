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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyEmploymentSummaryQueryService implements GetMyEmploymentSummaryUseCase {

    private final EmploymentSummaryPort employmentSummaryPort;
    private final Clock clock;

    @Override
    public MyEmploymentSummaryView getSummary(Long userId, Long academyId) {
        log.info("event=attendance_employment_summary_read_시작 userId={}, academyId={}", userId, academyId);
        LocalDate hireDate = employmentSummaryPort.findByUserIdAndAcademyId(userId, academyId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.EMPLOYMENT_INFO_NOT_FOUND))
                .hireDate();
        long tenureDays = Math.max(0, ChronoUnit.DAYS.between(hireDate, LocalDate.now(clock)));
        MyEmploymentSummaryView result = new MyEmploymentSummaryView(hireDate, tenureDays);
        log.info("event=attendance_employment_summary_read_완료 userId={}, academyId={}, tenureDays={}", userId, academyId, tenureDays);
        return result;
    }
}
