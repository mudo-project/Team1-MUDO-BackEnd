package com.academy.mudogroupware.payroll.domain.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.AttendanceResult;
import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.DailyAttendance;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayrollCalculatorTest {
  private final PayrollCalculator calculator = new PayrollCalculator();

  @Test
  void 월급제_계약변경은_달력일수로_분할하고_마지막에_반올림한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<CompensationData> contracts = List.of(
        compensation(1L, SalaryType.MONTHLY, "3100000", null,
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)),
        compensation(2L, SalaryType.MONTHLY, "3300000", null,
            LocalDate.of(2026, 8, 16), null));

    var result = calculator.calculate(month, contracts, List.of(), bd("20000"),
        attendance(List.of()), labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.BASE_SALARY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("3203226"));
  }

  @Test
  void 시급제_계약변경일을_기준으로_일자별_시급을_적용한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<CompensationData> contracts = List.of(
        compensation(1L, SalaryType.HOURLY, null, "10000",
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)),
        compensation(2L, SalaryType.HOURLY, null, "12000",
            LocalDate.of(2026, 8, 16), null));
    List<DailyAttendance> daily = List.of(
        new DailyAttendance(LocalDate.of(2026, 8, 10), 480, 0),
        new DailyAttendance(LocalDate.of(2026, 8, 20), 480, 0));

    var result = calculator.calculate(month, contracts, List.of(), bd("12000"),
        attendance(daily), labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.HOURLY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("176000"));
  }

  @Test
  void 휴일근로_8시간_초과기준은_근무일별로_적용한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<DailyAttendance> daily = List.of(
        new DailyAttendance(LocalDate.of(2026, 8, 2), 0, 600),
        new DailyAttendance(LocalDate.of(2026, 8, 9), 0, 600));

    var result = calculator.calculate(month,
        List.of(compensation(1L, SalaryType.MONTHLY, "3000000", null,
            month.atDay(1), null)), List.of(), bd("10000"), attendance(daily),
        labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.HOLIDAY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("320000"));
  }

  private CompensationData compensation(Long id, SalaryType salaryType, String salary,
      String hourly, LocalDate from, LocalDate to) {
    return new CompensationData(id, 10L, EmploymentType.REGULAR, salaryType,
        salary == null ? null : bd(salary), hourly == null ? null : bd(hourly),
        bd("40"), from, to);
  }
  private AttendanceResult attendance(List<DailyAttendance> daily) {
    long work = daily.stream().mapToLong(DailyAttendance::regularWorkMinutes).sum();
    long holiday = daily.stream().mapToLong(DailyAttendance::holidayMinutes).sum();
    return new AttendanceResult(daily.size(), work, 0, 0, holiday, 0, 0, daily);
  }
  private LaborScopeData labor() { return new LaborScopeData(1L, true); }
  private RuleData rules() { return new RuleData(bd("1.5"), bd("0.5"), bd("1.5"), bd("2.0")); }
  private InsuranceData insurance() { return new InsuranceData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO); }
  private TaxData tax() { return new TaxData(BigDecimal.ZERO, BigDecimal.ZERO); }
  private BigDecimal bd(String value) { return new BigDecimal(value); }
}
