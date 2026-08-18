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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollReferenceDataAdapter implements PayrollReferenceDataPort {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

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

  @Override
  public Map<Long, List<CompensationData>> findCompensations(
      Set<Long> userIds, YearMonth month) {
    if (userIds.isEmpty()) return Map.of();
    List<CompensationData> rows = namedJdbc.query("select * from employee_compensation "
            + "where user_id in (:userIds) and effective_from <= :monthEnd "
            + "and (effective_to is null or effective_to >= :monthStart) "
            + "order by user_id, effective_from",
        new MapSqlParameterSource()
            .addValue("userIds", userIds)
            .addValue("monthEnd", month.atEndOfMonth())
            .addValue("monthStart", month.atDay(1)),
        this::compensation);
    Map<Long, List<CompensationData>> result = new LinkedHashMap<>();
    for (CompensationData row : rows) {
      result.computeIfAbsent(row.userId(), ignored -> new java.util.ArrayList<>()).add(row);
    }
    return result;
  }

  @Override public List<CompensationData> findAllCompensations(Long userId) {
    return jdbc.query("select * from employee_compensation where user_id=? order by effective_from desc",
        this::compensation, userId);
  }

  @Override public CompensationData replaceCompensation(Long userId, CompensationData c) {
    if (c.id() == null) {
      jdbc.update("update employee_compensation set effective_to=? where user_id=? "
          + "and effective_to is null and effective_from < ?", c.effectiveFrom().minusDays(1),
          userId, c.effectiveFrom());
    }
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
      int changed = jdbc.update("update employee_compensation set employment_type=?, salary_type=?, base_salary=?, "
          + "hourly_wage=?, weekly_contract_hours=?, effective_from=?, effective_to=? "
          + "where compensation_id=? and user_id=?", c.employmentType().name(), c.salaryType().name(),
          c.baseSalary(), c.hourlyWage(), c.weeklyContractHours(), c.effectiveFrom(), c.effectiveTo(),
          c.id(), userId);
      if (changed == 0) {
        throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
            "급여 계약을 찾을 수 없습니다.");
      }
    }
    return findAllCompensations(userId).stream()
        .filter(saved -> saved.effectiveFrom().equals(c.effectiveFrom())).findFirst().orElseThrow();
  }

  @Override public List<AllowanceData> findAllowances(Long userId, YearMonth month) {
    return jdbc.query("select * from employee_fixed_allowance "
        + "where employee_id=? and effective_from <= ? and (effective_to is null or effective_to >= ?)",
        this::allowance,
        userId, month.atEndOfMonth(), month.atDay(1));
  }

  @Override public List<AllowanceData> findAllAllowances(Long userId) {
    return jdbc.query("select * from employee_fixed_allowance where employee_id=? "
        + "order by effective_from desc, allowance_id desc", this::allowance, userId);
  }

  @Override public List<AllowanceData> saveAllowances(Long userId, List<AllowanceData> allowances) {
    Set<Long> retainedIds = new HashSet<>();
    for (AllowanceData allowance : allowances) {
      if (allowance.id() != null) retainedIds.add(allowance.id());
    }
    if (retainedIds.isEmpty()) {
      jdbc.update("delete from employee_fixed_allowance where employee_id=?", userId);
    } else {
      namedJdbc.update("delete from employee_fixed_allowance "
          + "where employee_id=:employeeId and allowance_id not in (:retainedIds)",
          new MapSqlParameterSource("employeeId", userId).addValue("retainedIds", retainedIds));
    }
    for (AllowanceData allowance : allowances) {
      Integer overlaps = jdbc.queryForObject("select count(*) from employee_fixed_allowance "
          + "where employee_id=? and allowance_type=? and allowance_name=? "
          + "and effective_from <= coalesce(?, '9999-12-31') "
          + "and coalesce(effective_to, '9999-12-31') >= ? "
          + "and (? is null or allowance_id <> ?)", Integer.class, userId, allowance.type(),
          allowance.name(), allowance.effectiveTo(), allowance.effectiveFrom(),
          allowance.id(), allowance.id());
      if (overlaps != null && overlaps > 0) {
        throw new PayrollException(PayrollErrorCode.COMPENSATION_PERIOD_OVERLAP,
            "같은 고정수당의 적용 기간이 겹칩니다.");
      }
      if (allowance.id() == null) {
        jdbc.update("insert into employee_fixed_allowance (employee_id, allowance_type, "
            + "allowance_name, amount, effective_from, effective_to) values (?, ?, ?, ?, ?, ?)",
            userId, allowance.type(), allowance.name(), allowance.amount(),
            allowance.effectiveFrom(), allowance.effectiveTo());
      } else {
        int changed = jdbc.update("update employee_fixed_allowance set allowance_type=?, "
            + "allowance_name=?, amount=?, effective_from=?, effective_to=? "
            + "where allowance_id=? and employee_id=?", allowance.type(), allowance.name(),
            allowance.amount(), allowance.effectiveFrom(), allowance.effectiveTo(),
            allowance.id(), userId);
        if (changed == 0) throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
            "고정수당을 찾을 수 없습니다.");
      }
    }
    return findAllAllowances(userId);
  }

  @Override public List<PayBasisData> findPayBases(Long userId, YearMonth month) {
    return jdbc.query("select * from employee_pay_basis where employee_id=? "
        + "and effective_from <= ? and (effective_to is null or effective_to >= ?) "
        + "order by effective_from", this::payBasis, userId, month.atEndOfMonth(), month.atDay(1));
  }

  @Override public List<PayBasisData> findAllPayBases(Long userId) {
    return jdbc.query("select * from employee_pay_basis where employee_id=? "
        + "order by effective_from desc", this::payBasis, userId);
  }

  @Override public PayBasisData savePayBasis(Long userId, PayBasisData payBasis) {
    if (payBasis.id() == null) {
      jdbc.update("update employee_pay_basis set effective_to=? where employee_id=? "
          + "and effective_to is null and effective_from < ?", payBasis.effectiveFrom().minusDays(1),
          userId, payBasis.effectiveFrom());
    }
    Integer overlaps = jdbc.queryForObject("select count(*) from employee_pay_basis where employee_id=? "
        + "and effective_from <= coalesce(?, '9999-12-31') "
        + "and coalesce(effective_to, '9999-12-31') >= ? "
        + "and (? is null or pay_basis_id <> ?)", Integer.class, userId, payBasis.effectiveTo(),
        payBasis.effectiveFrom(), payBasis.id(), payBasis.id());
    if (overlaps != null && overlaps > 0) {
      throw new PayrollException(PayrollErrorCode.COMPENSATION_PERIOD_OVERLAP,
          "통상시급 적용 기간이 겹칩니다.");
    }
    if (payBasis.id() == null) {
      jdbc.update("insert into employee_pay_basis (employee_id, ordinary_hourly_wage, "
          + "effective_from, effective_to) values (?, ?, ?, ?)", userId,
          payBasis.ordinaryHourlyWage(), payBasis.effectiveFrom(), payBasis.effectiveTo());
    } else {
      int changed = jdbc.update("update employee_pay_basis set ordinary_hourly_wage=?, "
          + "effective_from=?, effective_to=? where pay_basis_id=? and employee_id=?",
          payBasis.ordinaryHourlyWage(), payBasis.effectiveFrom(), payBasis.effectiveTo(),
          payBasis.id(), userId);
      if (changed == 0) throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
          "통상시급 기준을 찾을 수 없습니다.");
    }
    return findAllPayBases(userId).stream()
        .filter(saved -> saved.effectiveFrom().equals(payBasis.effectiveFrom()))
        .findFirst().orElseThrow();
  }

  @Override public Optional<LaborScopeData> findLaborScope(YearMonth month) {
    return one("select labor_scope_id, is_five_or_more from workplace_labor_scope where `year_month`=?",
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
        + "employment_insurance_amount from social_insurance_assessment where employee_id=? and `year_month`=?",
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
        + "where employee_id=? and `year_month`=?",
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
  private AllowanceData allowance(ResultSet rs, int row) throws SQLException {
    return new AllowanceData(rs.getLong("allowance_id"), rs.getLong("employee_id"),
        rs.getString("allowance_type"), rs.getString("allowance_name"), rs.getBigDecimal("amount"),
        rs.getDate("effective_from").toLocalDate(),
        rs.getDate("effective_to") == null ? null : rs.getDate("effective_to").toLocalDate());
  }
  private PayBasisData payBasis(ResultSet rs, int row) throws SQLException {
    return new PayBasisData(rs.getLong("pay_basis_id"), rs.getLong("employee_id"),
        rs.getBigDecimal("ordinary_hourly_wage"), rs.getDate("effective_from").toLocalDate(),
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
