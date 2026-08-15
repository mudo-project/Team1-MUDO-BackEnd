package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.EmploymentType.REGULAR;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.SalaryType.MONTHLY;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.PayrollStatus.CONFIRMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort.EmployeeView;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.CompensationData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.domain.service.PayrollCalculator;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollServiceListQueryTest {
  @Mock PayrollRepository payrolls;
  @Mock PayrollReferenceDataPort referenceData;
  @Mock PayrollEmployeePort employees;
  @Mock PayrollAttendancePort attendance;
  @Mock PayrollStatementPort statements;
  @Mock PayrollCalculator calculator;
  @Mock PayrollConfirmationExecutor confirmationExecutor;
  private PayrollService service;

  @BeforeEach
  void setUp() {
    service = new PayrollService(payrolls, referenceData, employees, attendance, statements,
        calculator, confirmationExecutor);
  }

  @Test
  void 급여_목록의_보상정보를_직원별이_아닌_한번의_일괄_조회로_가져온다() {
    YearMonth month = YearMonth.of(2026, 8);
    var first = new EmployeeView(1L, "직원1", "one@example.com", LocalDate.of(2026, 1, 1));
    var second = new EmployeeView(2L, "직원2", "two@example.com", LocalDate.of(2026, 1, 1));
    var compensation = new CompensationData(10L, 1L, REGULAR, MONTHLY,
        new BigDecimal("3000000"), null, new BigDecimal("40"),
        LocalDate.of(2026, 1, 1), null);
    when(employees.findAllActive(null)).thenReturn(List.of(first, second));
    when(payrolls.findLatestByMonth(month)).thenReturn(List.of());
    when(referenceData.findCompensations(Set.of(1L, 2L), month))
        .thenReturn(Map.of(1L, List.of(compensation)));

    var result = service.list(month, null, null, null, 0, 20);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).employmentType()).isEqualTo(REGULAR);
    assertThat(result.content().get(1).employmentType()).isNull();
    verify(referenceData).findCompensations(Set.of(1L, 2L), month);
    verify(referenceData, never()).findCompensations(anyLong(), any(YearMonth.class));
  }

  @Test
  void Revision_목록의_Snapshot과_명세서를_Revision별이_아닌_일괄_조회한다() {
    YearMonth month = YearMonth.of(2026, 8);
    Payroll first = payroll(1L, 1);
    Payroll second = payroll(2L, 2);
    var employee = new EmployeeView(10L, "직원", "staff@example.com",
        LocalDate.of(2026, 1, 1));
    var attendanceSnapshot = new PayrollRepository.AttendanceSnapshot(20,
        new BigDecimal("160"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO);
    var ruleSnapshot = new PayrollRepository.RuleSnapshot(1L, true,
        new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("1.5"),
        new BigDecimal("2.0"));
    var bundle = new PayrollRepository.SnapshotBundle(
        attendanceSnapshot, List.of(), ruleSnapshot);
    when(payrolls.findById(1L)).thenReturn(java.util.Optional.of(first));
    when(payrolls.findRevisions(10L, month)).thenReturn(List.of(second, first));
    when(payrolls.findSnapshots(Set.of(1L, 2L))).thenReturn(Map.of(1L, bundle, 2L, bundle));
    when(statements.findByPayrollIds(Set.of(1L, 2L))).thenReturn(Map.of());
    when(employees.findById(10L)).thenReturn(java.util.Optional.of(employee));

    var result = service.revisions(1L);

    assertThat(result).hasSize(2);
    verify(payrolls).findSnapshots(Set.of(1L, 2L));
    verify(statements).findByPayrollIds(Set.of(1L, 2L));
    verify(payrolls, never()).findSnapshots(anyLong());
    verify(statements, never()).findByPayrollId(anyLong());
    verify(employees).findById(10L);
  }

  private Payroll payroll(Long id, int revisionNo) {
    return Payroll.restore(id, 10L, YearMonth.of(2026, 8), LocalDate.of(2026, 9, 5),
        CONFIRMED, new BigDecimal("3000000"), BigDecimal.ZERO,
        new BigDecimal("3000000"), revisionNo, revisionNo == 1 ? null : 1L, null,
        LocalDateTime.now(), LocalDateTime.now(), 1L, List.of());
  }
}
