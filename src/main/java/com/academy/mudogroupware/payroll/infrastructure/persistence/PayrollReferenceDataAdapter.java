package com.academy.mudogroupware.payroll.infrastructure.persistence;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollReferenceDataAdapter implements PayrollReferenceDataPort {
  private final JdbcTemplate jdbc;

  @Override public Optional<PayrollPolicyData> findPolicy() {
    return one("select * from payroll_policy order by payroll_policy_id desc limit 1",
        this::policy);
  }

  @Override public PayrollPolicyData savePolicy(PayrollPolicyData policy) {
    Optional<PayrollPolicyData> current = findPolicy();
    if (current.isEmpty()) {
      jdbc.update("insert into payroll_policy (pay_day_type, pay_day, payment_month_offset) values (?, ?, ?)",
          policy.payDayType().name(), policy.payDay(), policy.paymentMonthOffset());
    } else {
      jdbc.update("update payroll_policy set pay_day_type=?, pay_day=?, payment_month_offset=? "
          + "where payroll_policy_id=?", policy.payDayType().name(), policy.payDay(),
          policy.paymentMonthOffset(), current.get().id());
    }
    return findPolicy().orElseThrow();
  }

  @Override public List<CompensationData> findCompensations(Long userId, YearMonth month) {
    return jdbc.query("select * from employee_compensation where user_id=? "
        + "and effective_from <= ? and (effective_to is null or effective_to >= ?) order by effective_from",
        this::compensation, userId, month.atEndOfMonth(), month.atDay(1));
  }

  @Override public List<CompensationData> findAllCompensations(Long userId) {
    return jdbc.query("select * from employee_compensation where user_id=? order by effective_from desc",
        this::compensation, userId);
  }

  @Override public CompensationData replaceCompensation(Long userId, CompensationData c) {
    Integer overlaps = jdbc.queryForObject("select count(*) from employee_compensation where user_id=? "
        + "and effective_from <= coalesce(?, '9999-12-31') "
        + "and coalesce(effective_to, '9999-12-31') >= ? and (? is null or compensation_id <> ?)",
        Integer.class, userId, c.effectiveTo(), c.effectiveFrom(), c.id(), c.id());
    if (overlaps != null && overlaps > 0) {
      throw new PayrollException(PayrollErrorCode.COMPENSATION_PERIOD_OVERLAP);
    }
    if (c.id() == null) {
      jdbc.update("insert into employee_compensation (user_id, employment_type, salary_type, "
          + "base_salary, hourly_wage, weekly_contract_hours, effective_from, effective_to) "
          + "values (?, ?, ?, ?, ?, ?, ?, ?)", userId, c.employmentType().name(),
          c.salaryType().name(), c.baseSalary(), c.hourlyWage(), c.weeklyContractHours(),
          c.effectiveFrom(), c.effectiveTo());
    } else {
      jdbc.update("update employee_compensation set employment_type=?, salary_type=?, base_salary=?, "
          + "hourly_wage=?, weekly_contract_hours=?, effective_from=?, effective_to=? "
          + "where compensation_id=? and user_id=?", c.employmentType().name(), c.salaryType().name(),
          c.baseSalary(), c.hourlyWage(), c.weeklyContractHours(), c.effectiveFrom(), c.effectiveTo(),
          c.id(), userId);
    }
    return findAllCompensations(userId).stream()
        .filter(saved -> saved.effectiveFrom().equals(c.effectiveFrom())).findFirst().orElseThrow();
  }

  @Override public List<AllowanceData> findAllowances(Long userId, YearMonth month) {
    return jdbc.query("select allowance_type, allowance_name, amount from employee_fixed_allowance "
        + "where employee_id=? and effective_from <= ? and (effective_to is null or effective_to >= ?)",
        (rs, row) -> new AllowanceData(rs.getString(1), rs.getString(2), rs.getBigDecimal(3)),
        userId, month.atEndOfMonth(), month.atDay(1));
  }

  @Override public Optional<BigDecimal> findOrdinaryHourlyWage(Long userId, LocalDate date) {
    return one("select ordinary_hourly_wage from employee_pay_basis where employee_id=? "
        + "and effective_from <= ? and (effective_to is null or effective_to >= ?) "
        + "order by effective_from desc limit 1", (rs, row) -> rs.getBigDecimal(1), userId, date, date);
  }

  @Override public Optional<LaborScopeData> findLaborScope(YearMonth month) {
    return one("select labor_scope_id, is_five_or_more from workplace_labor_scope where year_month=?",
        (rs, row) -> new LaborScopeData(rs.getLong(1), rs.getBoolean(2)), month.atDay(1));
  }

  @Override public Optional<RuleData> findRules(LocalDate date) {
    List<PolicyRate> rates = jdbc.query("select policy_type, rate from statutory_policy "
        + "where policy_type in ('OVERTIME','NIGHT_WORK','HOLIDAY_WORK') and effective_from <= ? "
        + "and (effective_to is null or effective_to >= ?) order by effective_from desc",
        (rs, row) -> new PolicyRate(rs.getString(1), rs.getBigDecimal(2)), date, date);
    BigDecimal overtime = rate(rates, "OVERTIME");
    BigDecimal night = rate(rates, "NIGHT_WORK");
    BigDecimal holiday = rate(rates, "HOLIDAY_WORK");
    if (overtime == null || night == null || holiday == null) return Optional.empty();
    return Optional.of(new RuleData(overtime, night, holiday,
        holiday.add(new BigDecimal("0.5"))));
  }

  @Override public Optional<InsuranceData> findInsurance(Long userId, YearMonth month) {
    Optional<InsuranceStatuses> statuses = one("select national_pension_status, health_insurance_status, "
        + "employment_insurance_status from social_insurance_status where employee_id=? "
        + "and effective_from <= ? and (effective_to is null or effective_to >= ?) "
        + "order by effective_from desc limit 1",
        (rs, row) -> new InsuranceStatuses(rs.getString(1), rs.getString(2), rs.getString(3)),
        userId, month.atEndOfMonth(), month.atDay(1));
    if (statuses.isEmpty()) return Optional.empty();
    Optional<InsuranceData> assessment = one("select national_pension_amount, health_insurance_amount, long_term_care_amount, "
        + "employment_insurance_amount from social_insurance_assessment where employee_id=? and year_month=?",
        (rs, row) -> new InsuranceData(rs.getBigDecimal(1), rs.getBigDecimal(2),
            rs.getBigDecimal(3), rs.getBigDecimal(4)), userId, month.atDay(1));
    InsuranceStatuses status = statuses.get();
    if (assessment.isEmpty() && status.anyEnrolled()) return Optional.empty();
    InsuranceData source = assessment.orElse(new InsuranceData(BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO));
    return Optional.of(new InsuranceData(
        status.pension.equals("ENROLLED") ? source.nationalPension() : BigDecimal.ZERO,
        status.health.equals("ENROLLED") ? source.healthInsurance() : BigDecimal.ZERO,
        status.health.equals("ENROLLED") ? source.longTermCare() : BigDecimal.ZERO,
        status.employment.equals("ENROLLED") ? source.employmentInsurance() : BigDecimal.ZERO));
  }

  @Override public Optional<TaxData> findTax(Long userId, YearMonth month) {
    return one("select income_tax_amount, local_income_tax_amount from tax_assessment "
        + "where employee_id=? and year_month=?",
        (rs, row) -> new TaxData(rs.getBigDecimal(1), rs.getBigDecimal(2)), userId, month.atDay(1));
  }

  private PayrollPolicyData policy(ResultSet rs, int row) throws SQLException {
    return new PayrollPolicyData(rs.getLong("payroll_policy_id"),
        PayDayType.valueOf(rs.getString("pay_day_type")), (Integer) rs.getObject("pay_day"),
        rs.getInt("payment_month_offset"));
  }
  private CompensationData compensation(ResultSet rs, int row) throws SQLException {
    return new CompensationData(rs.getLong("compensation_id"), rs.getLong("user_id"),
        EmploymentType.valueOf(rs.getString("employment_type")),
        SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("base_salary"),
        rs.getBigDecimal("hourly_wage"), rs.getBigDecimal("weekly_contract_hours"),
        rs.getDate("effective_from").toLocalDate(),
        rs.getDate("effective_to") == null ? null : rs.getDate("effective_to").toLocalDate());
  }
  private BigDecimal rate(List<PolicyRate> rates, String type) {
    return rates.stream().filter(r -> r.type.equals(type)).map(PolicyRate::rate).findFirst().orElse(null);
  }
  private <T> Optional<T> one(String sql, org.springframework.jdbc.core.RowMapper<T> mapper,
      Object... args) {
    try { return Optional.ofNullable(jdbc.queryForObject(sql, mapper, args)); }
    catch (EmptyResultDataAccessException e) { return Optional.empty(); }
  }
  private record PolicyRate(String type, BigDecimal rate) {}
  private record InsuranceStatuses(String pension, String health, String employment) {
    boolean anyEnrolled() {
      return "ENROLLED".equals(pension) || "ENROLLED".equals(health)
          || "ENROLLED".equals(employment);
    }
  }
}
