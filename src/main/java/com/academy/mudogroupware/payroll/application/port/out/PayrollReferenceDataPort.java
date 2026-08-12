package com.academy.mudogroupware.payroll.application.port.out;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PayrollReferenceDataPort {
  Optional<PayrollPolicyData> findPolicy();
  PayrollPolicyData savePolicy(PayrollPolicyData policy);
  List<CompensationData> findCompensations(Long userId, YearMonth month);
  List<CompensationData> findAllCompensations(Long userId);
  CompensationData replaceCompensation(Long userId, CompensationData compensation);
  List<AllowanceData> findAllowances(Long userId, YearMonth month);
  Optional<BigDecimal> findOrdinaryHourlyWage(Long userId, LocalDate date);
  Optional<LaborScopeData> findLaborScope(YearMonth month);
  Optional<RuleData> findRules(LocalDate date);
  Optional<InsuranceData> findInsurance(Long userId, YearMonth month);
  Optional<TaxData> findTax(Long userId, YearMonth month);

  record PayrollPolicyData(Long id, PayDayType payDayType, Integer payDay, int paymentMonthOffset) {
    public LocalDate scheduledDate(YearMonth payrollMonth) {
      YearMonth paymentMonth = payrollMonth.plusMonths(paymentMonthOffset);
      int day = payDayType == PayDayType.MONTH_END ? paymentMonth.lengthOfMonth()
          : Math.min(payDay, paymentMonth.lengthOfMonth());
      return paymentMonth.atDay(day);
    }
  }

  record CompensationData(Long id, Long userId, EmploymentType employmentType,
      SalaryType salaryType, BigDecimal baseSalary, BigDecimal hourlyWage,
      BigDecimal weeklyContractHours, LocalDate effectiveFrom, LocalDate effectiveTo) {}

  record AllowanceData(String type, String name, BigDecimal amount) {}
  record LaborScopeData(Long id, boolean fiveOrMore) {}
  record RuleData(BigDecimal overtimeMultiplier, BigDecimal nightMultiplier,
      BigDecimal holidayUnder8Multiplier, BigDecimal holidayOver8Multiplier) {}
  record InsuranceData(BigDecimal nationalPension, BigDecimal healthInsurance,
      BigDecimal longTermCare, BigDecimal employmentInsurance) {}
  record TaxData(BigDecimal incomeTax, BigDecimal localIncomeTax) {}
}
