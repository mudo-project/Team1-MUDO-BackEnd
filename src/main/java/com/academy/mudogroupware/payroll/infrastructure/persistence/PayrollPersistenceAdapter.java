package com.academy.mudogroupware.payroll.infrastructure.persistence;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import com.academy.mudogroupware.payroll.domain.model.PayrollItem;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@Component
@RequiredArgsConstructor
public class PayrollPersistenceAdapter implements PayrollRepository {
  private final PayrollJpaRepository repository;
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  @Override
  public Payroll save(Payroll payroll) {
    PayrollJpaEntity entity;
    if (payroll.getId() == null) {
      entity = PayrollJpaEntity.create(payroll.getUserId(), payroll.getYearMonth().atDay(1),
          payroll.getScheduledPayDate(), payroll.getStatus(), payroll.getTotalEarnings(),
          payroll.getTotalDeductions(), payroll.getNetPay(), payroll.getRevisionNo(),
          payroll.getOriginalPayrollId(), payroll.getMemo(), payroll.getCalculatedAt(),
          payroll.getConfirmedAt());
    } else {
      entity = repository.findAggregateById(payroll.getId())
          .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND));
      if (entity.getVersion() != payroll.getVersion()) {
        throw new PayrollException(PayrollErrorCode.PAYROLL_VERSION_CONFLICT);
      }
      entity.apply(payroll.getStatus(), payroll.getTotalEarnings(), payroll.getTotalDeductions(),
          payroll.getNetPay(), payroll.getMemo(), payroll.getCalculatedAt(), payroll.getConfirmedAt());
    }
    entity.replaceItems(payroll.getItems());
    try {
      return toDomain(repository.saveAndFlush(entity));
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_VERSION_CONFLICT);
    } catch (DataIntegrityViolationException e) {
      PayrollErrorCode code = payroll.getRevisionNo() > 1
          ? PayrollErrorCode.PAYROLL_REVISION_CONFLICT
          : PayrollErrorCode.PAYROLL_ALREADY_EXISTS;
      throw new PayrollException(code);
    }
  }

  @Override public Optional<Payroll> findById(Long id) {
    return repository.findAggregateById(id).map(this::toDomain);
  }
  @Override public boolean existsInitial(Long userId, YearMonth month) {
    return repository.existsByUserIdAndYearMonthAndRevisionNo(userId, month.atDay(1), 1);
  }
  @Override public Optional<Payroll> findLatest(Long userId, YearMonth month) {
    return repository.findFirstByUserIdAndYearMonthOrderByRevisionNoDesc(userId, month.atDay(1))
        .map(this::toDomain);
  }
  @Override public List<Payroll> findLatestByMonth(YearMonth month) {
    return repository.findLatestByMonth(month.atDay(1)).stream().map(this::toDomain).toList();
  }
  @Override public List<Payroll> findRevisions(Long userId, YearMonth month) {
    return repository.findAllByUserIdAndYearMonthOrderByRevisionNoDesc(userId, month.atDay(1))
        .stream().map(this::toDomain).toList();
  }

  @Override
  public void replaceSnapshots(Long payrollId, SnapshotBundle bundle) {
    jdbc.update("delete from payroll_attendance_snapshot where payroll_id = ?", payrollId);
    jdbc.update("delete from payroll_compensation_snapshot where payroll_id = ?", payrollId);
    jdbc.update("delete from payroll_rule_snapshot where payroll_id = ?", payrollId);
    AttendanceSnapshot a = bundle.attendance();
    jdbc.update("insert into payroll_attendance_snapshot "
        + "(payroll_id, work_days, work_hours, overtime_hours, night_hours, holiday_hours, paid_leave_hours) "
        + "values (?, ?, ?, ?, ?, ?, ?)", payrollId, a.workDays(), a.workHours(),
        a.overtimeHours(), a.nightHours(), a.holidayHours(), a.paidLeaveHours());
    for (CompensationSnapshot c : bundle.compensations()) {
      jdbc.update("insert into payroll_compensation_snapshot "
          + "(payroll_id, applied_from, applied_to, employment_type, salary_type, base_salary, "
          + "hourly_wage, ordinary_hourly_wage, weekly_contract_hours) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          payrollId, c.appliedFrom(), c.appliedTo(), c.employmentType().name(), c.salaryType().name(),
          c.baseSalary(), c.hourlyWage(), c.ordinaryHourlyWage(), c.weeklyContractHours());
    }
    RuleSnapshot r = bundle.rule();
    jdbc.update("insert into payroll_rule_snapshot (payroll_id, labor_scope_id, is_five_or_more, "
        + "overtime_multiplier, night_multiplier, holiday_under_8_multiplier, holiday_over_8_multiplier) "
        + "values (?, ?, ?, ?, ?, ?, ?)", payrollId, r.laborScopeId(), r.fiveOrMore(),
        r.overtimeMultiplier(), r.nightMultiplier(), r.holidayUnder8Multiplier(),
        r.holidayOver8Multiplier());
  }

  @Override
  public SnapshotBundle findSnapshots(Long payrollId) {
    AttendanceSnapshot attendance = jdbc.queryForObject(
        "select * from payroll_attendance_snapshot where payroll_id = ?", this::attendance, payrollId);
    List<CompensationSnapshot> compensations = jdbc.query(
        "select * from payroll_compensation_snapshot where payroll_id = ? order by applied_from",
        this::compensation, payrollId);
    RuleSnapshot rule = jdbc.queryForObject(
        "select * from payroll_rule_snapshot where payroll_id = ?", this::rule, payrollId);
    return new SnapshotBundle(attendance, compensations, rule);
  }

  @Override
  public Map<Long, SnapshotBundle> findSnapshots(Set<Long> payrollIds) {
    if (payrollIds.isEmpty()) return Map.of();
    var parameters = new MapSqlParameterSource("payrollIds", payrollIds);
    Map<Long, AttendanceSnapshot> attendances = new LinkedHashMap<>();
    var attendanceRows = namedJdbc.query("select * from payroll_attendance_snapshot "
            + "where payroll_id in (:payrollIds)", parameters,
        (rs, row) -> Map.entry(rs.getLong("payroll_id"), attendance(rs, row)));
    for (var row : attendanceRows) attendances.put(row.getKey(), row.getValue());
    Map<Long, List<CompensationSnapshot>> compensations = new LinkedHashMap<>();
    var compensationRows = namedJdbc.query("select * from payroll_compensation_snapshot "
            + "where payroll_id in (:payrollIds) order by payroll_id, applied_from", parameters,
        (rs, row) -> Map.entry(rs.getLong("payroll_id"), compensation(rs, row)));
    for (var row : compensationRows) {
      compensations.computeIfAbsent(row.getKey(), ignored -> new ArrayList<>())
          .add(row.getValue());
    }
    Map<Long, RuleSnapshot> rules = new LinkedHashMap<>();
    var ruleRows = namedJdbc.query(
        "select * from payroll_rule_snapshot where payroll_id in (:payrollIds)", parameters,
        (rs, row) -> Map.entry(rs.getLong("payroll_id"), rule(rs, row)));
    for (var row : ruleRows) rules.put(row.getKey(), row.getValue());
    Map<Long, SnapshotBundle> result = new LinkedHashMap<>();
    for (Long payrollId : payrollIds) {
      AttendanceSnapshot attendance = attendances.get(payrollId);
      RuleSnapshot rule = rules.get(payrollId);
      if (attendance == null || rule == null) {
        throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
            "급여 Snapshot을 찾을 수 없습니다.");
      }
      result.put(payrollId, new SnapshotBundle(attendance,
          compensations.getOrDefault(payrollId, List.of()), rule));
    }
    return result;
  }

  private Payroll toDomain(PayrollJpaEntity e) {
    List<PayrollItem> items = e.getItems().stream().map(i -> new PayrollItem(i.getId(),
        i.getCategory(), i.getType(), i.getName(), i.getAmount(), i.getSourceType(),
        i.getOriginalAmount(), i.isAdjusted(), i.getAdjustmentReason(), i.getCalculationFormula(),
        i.getCalculationBasis(), i.getDisplayOrder())).toList();
    return Payroll.restore(e.getId(), e.getUserId(), YearMonth.from(e.getYearMonth()),
        e.getScheduledPayDate(), e.getStatus(), e.getTotalEarnings(), e.getTotalDeductions(),
        e.getNetPay(), e.getRevisionNo(), e.getOriginalPayrollId(), e.getMemo(),
        e.getCalculatedAt(), e.getConfirmedAt(), e.getVersion(), items);
  }

  private AttendanceSnapshot attendance(ResultSet rs, int row) throws SQLException {
    return new AttendanceSnapshot(rs.getInt("work_days"), rs.getBigDecimal("work_hours"),
        rs.getBigDecimal("overtime_hours"), rs.getBigDecimal("night_hours"),
        rs.getBigDecimal("holiday_hours"), rs.getBigDecimal("paid_leave_hours"));
  }
  private CompensationSnapshot compensation(ResultSet rs, int row) throws SQLException {
    return new CompensationSnapshot(rs.getDate("applied_from").toLocalDate(),
        rs.getDate("applied_to").toLocalDate(), EmploymentType.valueOf(rs.getString("employment_type")),
        SalaryType.valueOf(rs.getString("salary_type")), rs.getBigDecimal("base_salary"),
        rs.getBigDecimal("hourly_wage"), rs.getBigDecimal("ordinary_hourly_wage"),
        rs.getBigDecimal("weekly_contract_hours"));
  }
  private RuleSnapshot rule(ResultSet rs, int row) throws SQLException {
    return new RuleSnapshot(rs.getLong("labor_scope_id"), rs.getBoolean("is_five_or_more"),
        rs.getBigDecimal("overtime_multiplier"), rs.getBigDecimal("night_multiplier"),
        rs.getBigDecimal("holiday_under_8_multiplier"), rs.getBigDecimal("holiday_over_8_multiplier"));
  }
}
