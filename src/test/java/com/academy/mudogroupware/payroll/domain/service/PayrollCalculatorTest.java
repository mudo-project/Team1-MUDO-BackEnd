package com.academy.mudogroupware.payroll.domain.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.AttendanceResult;
import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.DailyAttendance;
import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort.WeeklyHoliday;
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

    var result = calculator.calculate(month, contracts, List.of(), payBasis("20000"),
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
        new DailyAttendance(LocalDate.of(2026, 8, 10), 480, 0, 0, 0),
        new DailyAttendance(LocalDate.of(2026, 8, 20), 480, 0, 0, 0));

    var result = calculator.calculate(month, contracts, List.of(), payBasis("12000"),
        attendance(daily), labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.HOURLY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("176000"));
  }

  @Test
  void 휴일근로_8시간_초과기준은_근무일별로_적용한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<DailyAttendance> daily = List.of(
        new DailyAttendance(LocalDate.of(2026, 8, 2), 0, 0, 0, 600),
        new DailyAttendance(LocalDate.of(2026, 8, 9), 0, 0, 0, 600));

    var result = calculator.calculate(month,
        List.of(compensation(1L, SalaryType.MONTHLY, "3000000", null,
            month.atDay(1), null)), List.of(), payBasis("10000"), attendance(daily),
        labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.HOLIDAY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("320000"));
  }

  @Test
  void 오인미만이어도_연장과_휴일근로의_기본임금은_지급하고_가산분만_제외한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<DailyAttendance> daily = List.of(
        new DailyAttendance(month.atDay(3), 420, 60, 60, 0),
        new DailyAttendance(month.atDay(9), 0, 0, 120, 600));
    AttendanceResult attendance = new AttendanceResult(1, 420, 60, 180, 600, 0,
        List.of(), daily);

    var result = calculator.calculate(month,
        List.of(compensation(1L, SalaryType.MONTHLY, "3000000", null,
            month.atDay(1), null)), List.of(), payBasis("10000"), attendance,
        new LaborScopeData(1L, false), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.OVERTIME_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("10000"));
    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.HOLIDAY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("100000"));
    assertThat(result.items()).noneMatch(i -> i.type() == ItemType.NIGHT_PAY);
  }

  @Test
  void 월중_통상시급_변경은_근로일자별로_적용한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<DailyAttendance> daily = List.of(
        new DailyAttendance(month.atDay(10), 420, 60, 0, 0),
        new DailyAttendance(month.atDay(20), 420, 60, 0, 0));
    AttendanceResult attendance = new AttendanceResult(2, 840, 120, 0, 0, 0,
        List.of(), daily);
    List<PayBasisData> bases = List.of(
        new PayBasisData(1L, 10L, bd("10000"), month.atDay(1), month.atDay(15)),
        new PayBasisData(2L, 10L, bd("12000"), month.atDay(16), null));

    var result = calculator.calculate(month,
        List.of(compensation(1L, SalaryType.MONTHLY, "3000000", null,
            month.atDay(1), null)), List.of(), bases, attendance,
        labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.OVERTIME_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("33000"));
    assertThat(result.snapshots().compensations()).hasSize(2);
  }

  @Test
  void 시급제_주휴수당은_해당주의_계약시급을_적용한다() {
    YearMonth month = YearMonth.of(2026, 8);
    List<CompensationData> contracts = List.of(
        compensation(1L, SalaryType.HOURLY, null, "10000", month.atDay(1), month.atDay(15)),
        compensation(2L, SalaryType.HOURLY, null, "12000", month.atDay(16), null));
    AttendanceResult attendance = new AttendanceResult(0, 0, 0, 0, 0, 0,
        List.of(new WeeklyHoliday(month.atDay(9), 480), new WeeklyHoliday(month.atDay(23), 480)),
        List.of());

    var result = calculator.calculate(month, contracts, List.of(), payBasis("12000"), attendance,
        labor(), rules(), insurance(), tax());

    assertThat(result.items()).filteredOn(i -> i.type() == ItemType.WEEKLY_HOLIDAY_PAY)
        .singleElement().extracting(i -> i.amount()).isEqualTo(bd("176000"));
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
    return new AttendanceResult(daily.size(), work, 0, 0, holiday, 0, List.of(), daily);
  }
  private List<PayBasisData> payBasis(String wage) {
    return List.of(new PayBasisData(1L, 10L, bd(wage), LocalDate.of(2026, 1, 1), null));
  }
  private LaborScopeData labor() { return new LaborScopeData(1L, true); }
  private RuleData rules() { return new RuleData(bd("1.5"), bd("0.5"), bd("1.5"), bd("2.0")); }
  private InsuranceData insurance() { return new InsuranceData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO); }
  private TaxData tax() { return new TaxData(BigDecimal.ZERO, BigDecimal.ZERO); }
  private BigDecimal bd(String value) { return new BigDecimal(value); }
}
