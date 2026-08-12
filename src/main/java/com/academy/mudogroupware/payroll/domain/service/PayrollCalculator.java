package com.academy.mudogroupware.payroll.domain.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.AttendanceResult;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.*;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository.*;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import com.academy.mudogroupware.payroll.domain.model.PayrollItem;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PayrollCalculator {
  private static final BigDecimal EIGHT_HOURS = BigDecimal.valueOf(8);

  public Calculation calculate(YearMonth month, List<CompensationData> contracts,
      List<AllowanceData> allowances, BigDecimal ordinaryHourlyWage,
      AttendanceResult attendance, LaborScopeData laborScope, RuleData rules,
      InsuranceData insurance, TaxData tax) {
    if (contracts.isEmpty() || ordinaryHourlyWage == null || laborScope == null || rules == null
        || insurance == null || tax == null) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING);
    }

    List<PayrollItem> items = new ArrayList<>();
    int order = 1;
    BigDecimal monthlyBase = calculateMonthlyBase(month, contracts);
    if (monthlyBase.signum() > 0) {
      items.add(item(ItemType.BASE_SALARY, "기본급", monthlyBase, SourceType.CONTRACT, order++));
    }
    BigDecimal hourlyPay = calculateHourlyPay(contracts, attendance);
    if (hourlyPay.signum() > 0) {
      items.add(item(ItemType.HOURLY_PAY, "시급", hourlyPay, SourceType.CONTRACT, order++));
    }
    for (AllowanceData allowance : allowances) {
      items.add(item(allowanceType(allowance.type()), allowance.name(), won(allowance.amount()),
          SourceType.CONTRACT, order++));
    }

    if (laborScope.fiveOrMore()) {
      order = addWorkPay(items, ItemType.OVERTIME_PAY, "연장근로수당", ordinaryHourlyWage,
          attendance.overtimeHours(), rules.overtimeMultiplier(), order);
      order = addWorkPay(items, ItemType.NIGHT_PAY, "야간근로수당", ordinaryHourlyWage,
          attendance.nightHours(), rules.nightMultiplier(), order);
      BigDecimal holiday = attendance.daily().stream().map(day -> {
        BigDecimal hours = BigDecimal.valueOf(day.holidayMinutes())
            .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal under8 = hours.min(EIGHT_HOURS);
        BigDecimal over8 = hours.subtract(under8).max(BigDecimal.ZERO);
        return ordinaryHourlyWage.multiply(under8).multiply(rules.holidayUnder8Multiplier())
            .add(ordinaryHourlyWage.multiply(over8).multiply(rules.holidayOver8Multiplier()));
      }).reduce(BigDecimal.ZERO, BigDecimal::add);
      if (holiday.signum() > 0) {
        items.add(workItem(ItemType.HOLIDAY_PAY, "휴일근로수당", won(holiday), ordinaryHourlyWage,
            attendance.holidayHours(), rules.holidayUnder8Multiplier(), order++));
      }
    }

    boolean hourly = contracts.stream().anyMatch(c -> c.salaryType() == SalaryType.HOURLY
        && c.weeklyContractHours().compareTo(BigDecimal.valueOf(15)) >= 0);
    if (hourly && attendance.weeklyHolidayHours().signum() > 0) {
      BigDecimal amount = won(ordinaryHourlyWage.multiply(attendance.weeklyHolidayHours()));
      items.add(workItem(ItemType.WEEKLY_HOLIDAY_PAY, "주휴수당", amount, ordinaryHourlyWage,
          attendance.weeklyHolidayHours(), BigDecimal.ONE, order++));
    }

    order = addDeduction(items, ItemType.NATIONAL_PENSION, "국민연금", insurance.nationalPension(),
        SourceType.MOCK_INSURANCE, order);
    order = addDeduction(items, ItemType.HEALTH_INSURANCE, "건강보험", insurance.healthInsurance(),
        SourceType.MOCK_INSURANCE, order);
    order = addDeduction(items, ItemType.LONG_TERM_CARE, "장기요양보험", insurance.longTermCare(),
        SourceType.MOCK_INSURANCE, order);
    order = addDeduction(items, ItemType.EMPLOYMENT_INSURANCE, "고용보험",
        insurance.employmentInsurance(), SourceType.MOCK_INSURANCE, order);
    order = addDeduction(items, ItemType.INCOME_TAX, "소득세", tax.incomeTax(),
        SourceType.MOCK_TAX, order);
    addDeduction(items, ItemType.LOCAL_INCOME_TAX, "지방소득세", tax.localIncomeTax(),
        SourceType.MOCK_TAX, order);

    AttendanceSnapshot attendanceSnapshot = new AttendanceSnapshot(attendance.workDays(),
        attendance.workHours(), attendance.overtimeHours(), attendance.nightHours(),
        attendance.holidayHours(), attendance.paidLeaveHours());
    List<CompensationSnapshot> compensationSnapshots = contracts.stream().map(contract -> {
      LocalDate from = contract.effectiveFrom().isBefore(month.atDay(1)) ? month.atDay(1)
          : contract.effectiveFrom();
      LocalDate to = contract.effectiveTo() == null || contract.effectiveTo().isAfter(month.atEndOfMonth())
          ? month.atEndOfMonth() : contract.effectiveTo();
      return new CompensationSnapshot(from, to, contract.employmentType(), contract.salaryType(),
          contract.baseSalary(), contract.hourlyWage(), ordinaryHourlyWage,
          contract.weeklyContractHours());
    }).toList();
    RuleSnapshot ruleSnapshot = new RuleSnapshot(laborScope.id(), laborScope.fiveOrMore(),
        rules.overtimeMultiplier(), rules.nightMultiplier(), rules.holidayUnder8Multiplier(),
        rules.holidayOver8Multiplier());
    return new Calculation(items,
        new SnapshotBundle(attendanceSnapshot, compensationSnapshots, ruleSnapshot));
  }

  private BigDecimal calculateMonthlyBase(YearMonth month, List<CompensationData> contracts) {
    BigDecimal total = BigDecimal.ZERO;
    for (CompensationData contract : contracts) {
      if (contract.salaryType() != SalaryType.MONTHLY || contract.baseSalary() == null) continue;
      LocalDate start = contract.effectiveFrom().isAfter(month.atDay(1))
          ? contract.effectiveFrom() : month.atDay(1);
      LocalDate end = contract.effectiveTo() != null && contract.effectiveTo().isBefore(month.atEndOfMonth())
          ? contract.effectiveTo() : month.atEndOfMonth();
      long days = ChronoUnit.DAYS.between(start, end) + 1;
      total = total.add(contract.baseSalary().multiply(BigDecimal.valueOf(days))
          .divide(BigDecimal.valueOf(month.lengthOfMonth()), MathContext.DECIMAL128));
    }
    return won(total);
  }

  private BigDecimal calculateHourlyPay(List<CompensationData> contracts, AttendanceResult attendance) {
    BigDecimal total = BigDecimal.ZERO;
    for (var day : attendance.daily()) {
      CompensationData contract = contracts.stream()
          .filter(c -> c.salaryType() == SalaryType.HOURLY)
          .filter(c -> !day.date().isBefore(c.effectiveFrom()))
          .filter(c -> c.effectiveTo() == null || !day.date().isAfter(c.effectiveTo()))
          .findFirst().orElse(null);
      if (contract == null || day.regularWorkMinutes() == 0) continue;
      BigDecimal hours = BigDecimal.valueOf(day.regularWorkMinutes())
          .divide(BigDecimal.valueOf(60), MathContext.DECIMAL128);
      total = total.add(contract.hourlyWage().multiply(hours));
    }
    return won(total);
  }

  private int addWorkPay(List<PayrollItem> items, ItemType type, String name, BigDecimal wage,
      BigDecimal hours, BigDecimal multiplier, int order) {
    BigDecimal amount = won(wage.multiply(hours).multiply(multiplier));
    if (amount.signum() > 0) items.add(workItem(type, name, amount, wage, hours, multiplier, order++));
    return order;
  }

  private PayrollItem workItem(ItemType type, String name, BigDecimal amount, BigDecimal wage,
      BigDecimal hours, BigDecimal multiplier, int order) {
    String formula = "%s원 × %s시간 × %s".formatted(wage.stripTrailingZeros().toPlainString(),
        hours.stripTrailingZeros().toPlainString(), multiplier.stripTrailingZeros().toPlainString());
    String basis = "{\"ordinaryHourlyWage\":%s,\"hours\":%s,\"multiplier\":%s}".formatted(
        wage.toPlainString(), hours.toPlainString(), multiplier.toPlainString());
    return new PayrollItem(null, ItemCategory.EARNING, type, name, amount, SourceType.ATTENDANCE,
        null, false, null, formula, basis, order);
  }

  private int addDeduction(List<PayrollItem> items, ItemType type, String name, BigDecimal amount,
      SourceType source, int order) {
    if (amount != null && amount.signum() > 0) {
      items.add(new PayrollItem(null, ItemCategory.DEDUCTION, type, name, won(amount), source,
          null, false, null, null, null, order++));
    }
    return order;
  }

  private PayrollItem item(ItemType type, String name, BigDecimal amount, SourceType source, int order) {
    return new PayrollItem(null, ItemCategory.EARNING, type, name, amount, source,
        null, false, null, null, null, order);
  }

  private ItemType allowanceType(String type) {
    return switch (type) {
      case "MEAL" -> ItemType.MEAL_ALLOWANCE;
      case "POSITION" -> ItemType.POSITION_ALLOWANCE;
      case "DUTY" -> ItemType.DUTY_ALLOWANCE;
      case "TRANSPORTATION" -> ItemType.TRANSPORTATION_ALLOWANCE;
      default -> ItemType.OTHER_ALLOWANCE;
    };
  }

  private BigDecimal won(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP);
  }

  public record Calculation(List<PayrollItem> items, SnapshotBundle snapshots) {}
}
