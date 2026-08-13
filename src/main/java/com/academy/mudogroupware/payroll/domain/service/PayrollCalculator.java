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
      List<AllowanceData> allowances, List<PayBasisData> payBases,
      AttendanceResult attendance, LaborScopeData laborScope, RuleData rules,
      InsuranceData insurance, TaxData tax) {
    if (contracts.isEmpty() || payBases.isEmpty() || laborScope == null || rules == null
        || insurance == null || tax == null) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING);
    }
    validatePayBasisCoverage(month, contracts, payBases);

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

    BigDecimal overtimeMultiplier = laborScope.fiveOrMore()
        ? rules.overtimeMultiplier() : BigDecimal.ONE;
    BigDecimal overtime = attendance.daily().stream()
        .map(day -> amount(payBases, day.date(), day.overtimeMinutes(), overtimeMultiplier))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (overtime.signum() > 0) {
      items.add(aggregateWorkItem(ItemType.OVERTIME_PAY, "연장근로수당", won(overtime),
          attendance.overtimeHours(), overtimeMultiplier, order++));
    }
    if (laborScope.fiveOrMore()) {
      BigDecimal night = attendance.daily().stream()
          .map(day -> amount(payBases, day.date(), day.nightMinutes(), rules.nightMultiplier()))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (night.signum() > 0) {
        items.add(aggregateWorkItem(ItemType.NIGHT_PAY, "야간근로수당", won(night),
            attendance.nightHours(), rules.nightMultiplier(), order++));
      }
    }
    BigDecimal holidayUnderMultiplier = laborScope.fiveOrMore()
        ? rules.holidayUnder8Multiplier() : BigDecimal.ONE;
    BigDecimal holidayOverMultiplier = laborScope.fiveOrMore()
        ? rules.holidayOver8Multiplier() : BigDecimal.ONE;
    BigDecimal holiday = attendance.daily().stream().map(day -> {
      BigDecimal wage = wageAt(payBases, day.date());
      BigDecimal hours = hours(day.holidayMinutes());
      BigDecimal under8 = hours.min(EIGHT_HOURS);
      BigDecimal over8 = hours.subtract(under8).max(BigDecimal.ZERO);
      return wage.multiply(under8).multiply(holidayUnderMultiplier)
          .add(wage.multiply(over8).multiply(holidayOverMultiplier));
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (holiday.signum() > 0) {
      items.add(aggregateHolidayItem(won(holiday), attendance.holidayHours(),
          holidayUnderMultiplier, holidayOverMultiplier, order++));
    }

    BigDecimal weeklyHoliday = attendance.weeklyHolidays().stream().map(holidayDay -> {
      CompensationData contract = contractAt(contracts, holidayDay.date(), SalaryType.HOURLY);
      if (contract == null || contract.weeklyContractHours().compareTo(BigDecimal.valueOf(15)) < 0) {
        return BigDecimal.ZERO;
      }
      return contract.hourlyWage().multiply(hours(holidayDay.minutes()));
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (weeklyHoliday.signum() > 0) {
      items.add(new PayrollItem(null, ItemCategory.EARNING, ItemType.WEEKLY_HOLIDAY_PAY,
          "주휴수당", won(weeklyHoliday), SourceType.ATTENDANCE, null, false, null,
          "주별 시급 × 1일 소정근로시간 합계",
          "{\"weeklyHolidayHours\":%s}".formatted(attendance.weeklyHolidayHours().toPlainString()),
          order++));
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
    List<CompensationSnapshot> compensationSnapshots = contracts.stream().flatMap(contract -> {
      LocalDate from = contract.effectiveFrom().isBefore(month.atDay(1)) ? month.atDay(1)
          : contract.effectiveFrom();
      LocalDate to = contract.effectiveTo() == null || contract.effectiveTo().isAfter(month.atEndOfMonth())
          ? month.atEndOfMonth() : contract.effectiveTo();
      return payBases.stream().filter(basis -> overlaps(from, to, basis.effectiveFrom(), basis.effectiveTo()))
          .map(basis -> new CompensationSnapshot(max(from, basis.effectiveFrom()),
              min(to, basis.effectiveTo()), contract.employmentType(), contract.salaryType(),
              contract.baseSalary(), contract.hourlyWage(), basis.ordinaryHourlyWage(),
              contract.weeklyContractHours()));
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
      CompensationData contract = contractAt(contracts, day.date(), SalaryType.HOURLY);
      if (contract == null || day.regularWorkMinutes() == 0) continue;
      BigDecimal hours = BigDecimal.valueOf(day.regularWorkMinutes())
          .divide(BigDecimal.valueOf(60), MathContext.DECIMAL128);
      total = total.add(contract.hourlyWage().multiply(hours));
    }
    return won(total);
  }

  private BigDecimal amount(List<PayBasisData> payBases, LocalDate date, long minutes,
      BigDecimal multiplier) {
    return wageAt(payBases, date).multiply(hours(minutes)).multiply(multiplier);
  }

  private BigDecimal wageAt(List<PayBasisData> payBases, LocalDate date) {
    return payBases.stream().filter(b -> !date.isBefore(b.effectiveFrom()))
        .filter(b -> b.effectiveTo() == null || !date.isAfter(b.effectiveTo()))
        .findFirst().map(PayBasisData::ordinaryHourlyWage)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
            date + "에 적용할 통상시급 기준이 없습니다."));
  }

  private void validatePayBasisCoverage(YearMonth month, List<CompensationData> contracts,
      List<PayBasisData> payBases) {
    for (CompensationData contract : contracts) {
      LocalDate start = max(month.atDay(1), contract.effectiveFrom());
      LocalDate end = min(month.atEndOfMonth(), contract.effectiveTo());
      for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        wageAt(payBases, date);
      }
    }
  }

  private CompensationData contractAt(List<CompensationData> contracts, LocalDate date,
      SalaryType salaryType) {
    return contracts.stream().filter(c -> c.salaryType() == salaryType)
        .filter(c -> !date.isBefore(c.effectiveFrom()))
        .filter(c -> c.effectiveTo() == null || !date.isAfter(c.effectiveTo()))
        .findFirst().orElse(null);
  }

  private BigDecimal hours(long minutes) {
    return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), MathContext.DECIMAL128);
  }

  private PayrollItem aggregateWorkItem(ItemType type, String name, BigDecimal amount,
      BigDecimal hours, BigDecimal multiplier, int order) {
    String formula = "일자별 통상시급 × %s시간 × %s 합계".formatted(
        hours.stripTrailingZeros().toPlainString(), multiplier.stripTrailingZeros().toPlainString());
    String basis = "{\"hours\":%s,\"multiplier\":%s,\"payBasisAppliedByDate\":true}".formatted(
        hours.toPlainString(), multiplier.toPlainString());
    return new PayrollItem(null, ItemCategory.EARNING, type, name, amount, SourceType.ATTENDANCE,
        null, false, null, formula, basis, order);
  }

  private PayrollItem aggregateHolidayItem(BigDecimal amount, BigDecimal hours,
      BigDecimal underMultiplier, BigDecimal overMultiplier, int order) {
    String formula = "일자별 통상시급 × (8시간 이내 × %s + 8시간 초과 × %s) 합계".formatted(
        underMultiplier.stripTrailingZeros().toPlainString(),
        overMultiplier.stripTrailingZeros().toPlainString());
    String basis = ("{\"holidayHours\":%s,\"under8Multiplier\":%s,\"over8Multiplier\":%s,"
        + "\"payBasisAppliedByDate\":true}").formatted(hours.toPlainString(),
            underMultiplier.toPlainString(), overMultiplier.toPlainString());
    return new PayrollItem(null, ItemCategory.EARNING, ItemType.HOLIDAY_PAY, "휴일근로수당",
        amount, SourceType.ATTENDANCE, null, false, null, formula, basis, order);
  }

  private boolean overlaps(LocalDate firstFrom, LocalDate firstTo, LocalDate secondFrom,
      LocalDate secondTo) {
    return !firstFrom.isAfter(secondTo == null ? LocalDate.MAX : secondTo)
        && !secondFrom.isAfter(firstTo);
  }

  private LocalDate max(LocalDate first, LocalDate second) {
    return first.isAfter(second) ? first : second;
  }

  private LocalDate min(LocalDate first, LocalDate second) {
    if (second == null) return first;
    return first.isBefore(second) ? first : second;
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
