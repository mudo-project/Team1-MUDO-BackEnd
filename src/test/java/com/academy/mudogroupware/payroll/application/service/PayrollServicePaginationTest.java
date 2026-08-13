package com.academy.mudogroupware.payroll.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort.EmployeeView;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.domain.service.PayrollCalculator;

class PayrollServicePaginationTest {

  @Test
  void 월별_급여_목록에_전체_페이지_정보를_반환한다() {
    PayrollRepository payrollRepository = mock(PayrollRepository.class);
    PayrollReferenceDataPort referenceData = mock(PayrollReferenceDataPort.class);
    PayrollEmployeePort employeePort = mock(PayrollEmployeePort.class);
    YearMonth month = YearMonth.of(2026, 8);
    when(employeePort.findAllActive(null)).thenReturn(List.of(
        new EmployeeView(1L, "직원1", "one@example.com", LocalDate.of(2026, 1, 1)),
        new EmployeeView(2L, "직원2", "two@example.com", LocalDate.of(2026, 1, 1))));
    when(payrollRepository.findLatestByMonth(month)).thenReturn(List.of());
    when(referenceData.findCompensations(anyLong(), eq(month))).thenReturn(List.of());
    PayrollService service = new PayrollService(
        payrollRepository,
        referenceData,
        employeePort,
        mock(PayrollAttendancePort.class),
        mock(PayrollStatementPort.class),
        mock(PayrollCalculator.class),
        mock(PayrollConfirmationExecutor.class));

    var result = service.list(month, null, null, null, 0, 1);

    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.totalPages()).isEqualTo(2);
    assertThat(result.first()).isTrue();
    assertThat(result.last()).isFalse();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.hasPrevious()).isFalse();
  }
}
