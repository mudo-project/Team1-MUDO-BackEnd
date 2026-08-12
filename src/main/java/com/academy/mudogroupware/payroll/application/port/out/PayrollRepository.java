package com.academy.mudogroupware.payroll.application.port.out;

import com.academy.mudogroupware.payroll.domain.model.Payroll;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.PayrollStatus;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository {
  Payroll save(Payroll payroll);
  Optional<Payroll> findById(Long id);
  boolean existsInitial(Long userId, YearMonth yearMonth);
  Optional<Payroll> findLatest(Long userId, YearMonth yearMonth);
  List<Payroll> findLatestByMonth(YearMonth yearMonth);
  List<Payroll> findRevisions(Long userId, YearMonth yearMonth);
  void replaceSnapshots(Long payrollId, SnapshotBundle snapshots);
  SnapshotBundle findSnapshots(Long payrollId);

  record AttendanceSnapshot(int workDays, java.math.BigDecimal workHours,
      java.math.BigDecimal overtimeHours, java.math.BigDecimal nightHours,
      java.math.BigDecimal holidayHours, java.math.BigDecimal paidLeaveHours) {}
  record CompensationSnapshot(java.time.LocalDate appliedFrom, java.time.LocalDate appliedTo,
      com.academy.mudogroupware.payroll.domain.model.PayrollTypes.EmploymentType employmentType,
      com.academy.mudogroupware.payroll.domain.model.PayrollTypes.SalaryType salaryType,
      java.math.BigDecimal baseSalary, java.math.BigDecimal hourlyWage,
      java.math.BigDecimal ordinaryHourlyWage, java.math.BigDecimal weeklyContractHours) {}
  record RuleSnapshot(Long laborScopeId, boolean fiveOrMore,
      java.math.BigDecimal overtimeMultiplier, java.math.BigDecimal nightMultiplier,
      java.math.BigDecimal holidayUnder8Multiplier, java.math.BigDecimal holidayOver8Multiplier) {}
  record SnapshotBundle(AttendanceSnapshot attendance, List<CompensationSnapshot> compensations,
      RuleSnapshot rule) {}
}
