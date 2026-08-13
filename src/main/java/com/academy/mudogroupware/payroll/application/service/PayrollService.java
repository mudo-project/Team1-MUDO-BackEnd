package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.global.domain.common.page.PagedResult;
import com.academy.mudogroupware.payroll.application.port.out.*;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort.EmployeeView;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.*;
import com.academy.mudogroupware.payroll.application.result.*;
import com.academy.mudogroupware.payroll.domain.exception.*;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import com.academy.mudogroupware.payroll.domain.service.PayrollCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {
  private final PayrollRepository payrollRepository;
  private final PayrollReferenceDataPort referenceData;
  private final PayrollEmployeePort employeePort;
  private final PayrollAttendancePort attendancePort;
  private final PayrollStatementPort statementPort;
  private final PayrollCalculator calculator;
  private final PayrollConfirmationExecutor confirmationExecutor;

  public PayrollDetailResult create(Long employeeId, YearMonth month) {
    EmployeeView employee = activeEmployee(employeeId);
    if (payrollRepository.existsInitial(employeeId, month)) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_ALREADY_EXISTS);
    }
    PayrollPolicyData policy = referenceData.findPolicy()
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_POLICY_NOT_FOUND));
    Payroll saved = payrollRepository.save(Payroll.draft(employeeId, month, policy.scheduledDate(month)));
    return PayrollDetailResult.of(saved, employee, null, null);
  }

  public PayrollDetailResult calculate(Long payrollId, long expectedVersion) {
    Payroll payroll = payroll(payrollId);
    checkVersion(payroll, expectedVersion);
    List<CompensationData> contracts = referenceData.findCompensations(
        payroll.getUserId(), payroll.getYearMonth());
    if (contracts.isEmpty()) throw new PayrollException(PayrollErrorCode.COMPENSATION_NOT_FOUND);
    List<PayBasisData> payBases = referenceData.findPayBases(
        payroll.getUserId(), payroll.getYearMonth());
    if (payBases.isEmpty()) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
          "통상시급 기준이 없습니다.");
    }
    var calculation = calculator.calculate(payroll.getYearMonth(), contracts,
        referenceData.findAllowances(payroll.getUserId(), payroll.getYearMonth()), payBases,
        attendancePort.getMonthlyAttendance(payroll.getUserId(), payroll.getYearMonth()),
        referenceData.findLaborScope(payroll.getYearMonth()).orElse(null),
        referenceData.findRules(payroll.getYearMonth().atEndOfMonth()).orElse(null),
        referenceData.findInsurance(payroll.getUserId(), payroll.getYearMonth()).orElse(null),
        referenceData.findTax(payroll.getUserId(), payroll.getYearMonth()).orElse(null));
    payroll.replaceCalculatedItems(calculation.items(), LocalDateTime.now());
    Payroll saved = payrollRepository.save(payroll);
    payrollRepository.replaceSnapshots(saved.getId(), calculation.snapshots());
    return detail(saved, calculation.snapshots());
  }

  public PayrollDetailResult update(Long payrollId, long expectedVersion, String memo,
      List<ItemAdjustment> adjustments) {
    Payroll payroll = payroll(payrollId);
    checkVersion(payroll, expectedVersion);
    if (adjustments != null) adjustments.forEach(a -> payroll.adjustItem(a.itemId(), a.amount(), a.reason()));
    payroll.updateMemo(memo);
    Payroll saved = payrollRepository.save(payroll);
    return detail(saved, snapshots(saved));
  }

  public PayrollDetailResult addEarning(Long payrollId, long expectedVersion, String name,
      BigDecimal amount) {
    Payroll payroll = payroll(payrollId);
    checkVersion(payroll, expectedVersion);
    payroll.addManualEarning(name, amount);
    Payroll saved = payrollRepository.save(payroll);
    return detail(saved, snapshots(saved));
  }

  public PayrollDetailResult deleteEarning(Long payrollId, Long itemId, long expectedVersion) {
    Payroll payroll = payroll(payrollId);
    checkVersion(payroll, expectedVersion);
    payroll.removeManualEarning(itemId);
    Payroll saved = payrollRepository.save(payroll);
    return detail(saved, snapshots(saved));
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public PayrollDetailResult confirm(Long payrollId, long expectedVersion) {
    Payroll payroll = payroll(payrollId);
    if (payroll.getStatus() == PayrollStatus.CONFIRMED) return detail(payroll, snapshots(payroll));
    try {
      Payroll saved = confirmationExecutor.confirm(payrollId, expectedVersion);
      return detail(saved, snapshots(saved));
    } catch (PayrollException e) {
      if (e.getErrorCode() != PayrollErrorCode.PAYROLL_VERSION_CONFLICT) throw e;
      Payroll current = payroll(payrollId);
      if (current.getStatus() != PayrollStatus.CONFIRMED) throw e;
      return detail(current, snapshots(current));
    }
  }

  public PayrollDetailResult createRevision(Long payrollId, long expectedVersion) {
    Payroll original = payroll(payrollId);
    checkVersion(original, expectedVersion);
    Payroll latest = payrollRepository.findLatest(original.getUserId(), original.getYearMonth())
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND));
    if (!latest.getId().equals(original.getId())) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_REVISION_CONFLICT);
    }
    Payroll revision = payrollRepository.save(original.revision());
    payrollRepository.replaceSnapshots(revision.getId(), snapshots(original));
    return detail(revision, snapshots(revision));
  }

  @Transactional(readOnly = true)
  public PayrollDetailResult get(Long payrollId) {
    Payroll payroll = payroll(payrollId);
    return detail(payroll, payroll.getStatus() == PayrollStatus.DRAFT ? null : snapshots(payroll));
  }

  @Transactional(readOnly = true)
  public PayrollDetailResult preview(Long payrollId) {
    Payroll payroll = payroll(payrollId);
    if (payroll.getStatus() == PayrollStatus.DRAFT) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_STATE);
    }
    return detail(payroll, snapshots(payroll));
  }

  @Transactional(readOnly = true)
  public List<PayrollDetailResult> revisions(Long payrollId) {
    Payroll current = payroll(payrollId);
    return payrollRepository.findRevisions(current.getUserId(), current.getYearMonth()).stream()
        .map(p -> detail(p, snapshots(p))).toList();
  }

  @Transactional(readOnly = true)
  public PayrollListResult list(YearMonth month, EmploymentType employmentType, String status,
      String name, int page, int size) {
    if (status != null && !status.isBlank()
        && !Set.of("NOT_CREATED", "DRAFT", "CALCULATED", "CONFIRMED").contains(status)) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_REQUEST,
          "급여 준비 상태가 올바르지 않습니다.");
    }
    List<EmployeeView> employees = employeePort.findAllActive(name);
    Map<Long, Payroll> payrolls = payrollRepository.findLatestByMonth(month).stream()
        .collect(Collectors.toMap(Payroll::getUserId, Function.identity()));
    List<PayrollListResult.Row> filteredRows = employees.stream().map(employee -> {
      Payroll p = payrolls.get(employee.id());
      EmploymentType type = referenceData.findCompensations(employee.id(), month).stream()
          .reduce((first, second) -> second).map(CompensationData::employmentType).orElse(null);
      return new PayrollListResult.Row(employee.id(), employee.name(), type,
          p == null ? null : p.getId(), p == null ? "NOT_CREATED" : p.getStatus().name(),
          p == null ? null : p.getTotalEarnings(), p == null ? null : p.getTotalDeductions(),
          p == null ? null : p.getNetPay(), p == null ? 0 : p.getRevisionNo());
    }).filter(row -> employmentType == null || row.employmentType() == employmentType)
        .filter(row -> status == null || status.isBlank() || row.preparationStatus().equals(status))
        .toList();
    int from = Math.min(page * size, filteredRows.size());
    int to = Math.min(from + size, filteredRows.size());
    List<PayrollListResult.Row> rows = filteredRows.subList(from, to);
    long notCreated = filteredRows.stream().filter(r -> r.payrollId() == null).count();
    long calculated = filteredRows.stream().filter(r -> "CALCULATED".equals(r.preparationStatus())).count();
    long confirmed = filteredRows.stream().filter(r -> "CONFIRMED".equals(r.preparationStatus())).count();
    BigDecimal earnings = sum(filteredRows, PayrollListResult.Row::totalEarnings);
    BigDecimal deductions = sum(filteredRows, PayrollListResult.Row::totalDeductions);
    BigDecimal net = sum(filteredRows, PayrollListResult.Row::netPay);
    PagedResult<PayrollListResult.Row> result = PagedResult.of(rows, page, size, filteredRows.size());
    return new PayrollListResult(result.content(), result.page(), result.size(),
        result.totalElements(), result.totalPages(), result.first(), result.last(),
        result.hasNext(), result.hasPrevious(),
        new PayrollListResult.Summary(filteredRows.size(), notCreated, calculated, confirmed,
            earnings, deductions, net));
  }

  public PayrollPolicyData updatePolicy(PayrollPolicyData policy) {
    if (policy == null || policy.payDayType() == null
        || policy.payDayType() == PayDayType.FIXED_DAY
            && (policy.payDay() == null || policy.payDay() < 1 || policy.payDay() > 31)
        || policy.payDayType() == PayDayType.MONTH_END && policy.payDay() != null
        || policy.paymentMonthOffset() < 0 || policy.paymentMonthOffset() > 12) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_REQUEST);
    }
    return referenceData.savePolicy(policy);
  }
  @Transactional(readOnly = true) public PayrollPolicyData getPolicy() {
    return referenceData.findPolicy().orElseThrow(
        () -> new PayrollException(PayrollErrorCode.PAYROLL_POLICY_NOT_FOUND));
  }
  public EmployeePayrollSettingsResult saveEmployeeSettings(Long employeeId,
      CompensationData compensation, List<AllowanceData> allowances, PayBasisData payBasis) {
    activeEmployee(employeeId);
    if (compensation != null) {
      validateCompensation(compensation);
      referenceData.replaceCompensation(employeeId, compensation);
    }
    if (allowances != null) {
      allowances.forEach(this::validateAllowance);
      referenceData.saveAllowances(employeeId, allowances);
    }
    if (payBasis != null) {
      validatePayBasis(payBasis);
      referenceData.savePayBasis(employeeId, payBasis);
    }
    return employeeSettings(employeeId);
  }
  @Transactional(readOnly = true)
  public EmployeePayrollSettingsResult employeeSettings(Long employeeId) {
    employee(employeeId);
    return new EmployeePayrollSettingsResult(employeeId,
        referenceData.findAllCompensations(employeeId),
        referenceData.findAllAllowances(employeeId),
        referenceData.findAllPayBases(employeeId));
  }

  private void validateCompensation(CompensationData c) {
    boolean invalidMonthly = c.salaryType() == SalaryType.MONTHLY
        && (c.baseSalary() == null || c.baseSalary().signum() < 0);
    boolean invalidHourly = c.salaryType() == SalaryType.HOURLY
        && (c.hourlyWage() == null || c.hourlyWage().signum() < 0);
    if (c.employmentType() == null || c.salaryType() == null || invalidMonthly || invalidHourly
        || c.weeklyContractHours() == null
        || c.weeklyContractHours().signum() < 0
        || c.weeklyContractHours().compareTo(BigDecimal.valueOf(168)) > 0
        || c.effectiveFrom() == null
        || c.effectiveTo() != null && c.effectiveTo().isBefore(c.effectiveFrom())) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_REQUEST);
    }
  }
  private void validateAllowance(AllowanceData allowance) {
    Set<String> types = Set.of("MEAL", "POSITION", "DUTY", "TRANSPORTATION", "OTHER");
    if (!types.contains(allowance.type()) || allowance.name() == null || allowance.name().isBlank()
        || allowance.name().length() > 100 || allowance.amount() == null
        || allowance.amount().signum() < 0 || allowance.effectiveFrom() == null
        || allowance.effectiveTo() != null
            && allowance.effectiveTo().isBefore(allowance.effectiveFrom())) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_REQUEST);
    }
  }
  private void validatePayBasis(PayBasisData payBasis) {
    if (payBasis.ordinaryHourlyWage() == null || payBasis.ordinaryHourlyWage().signum() <= 0
        || payBasis.effectiveFrom() == null || payBasis.effectiveTo() != null
            && payBasis.effectiveTo().isBefore(payBasis.effectiveFrom())) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_REQUEST);
    }
  }
  private Payroll payroll(Long id) { return payrollRepository.findById(id)
      .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND)); }
  private EmployeeView employee(Long id) { return employeePort.findById(id)
      .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND, "직원을 찾을 수 없습니다.")); }
  private EmployeeView activeEmployee(Long id) { return employeePort.findActiveById(id)
      .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND,
          "활성 직원을 찾을 수 없습니다.")); }
  private void checkVersion(Payroll payroll, long expected) {
    if (payroll.getVersion() != expected) throw new PayrollException(PayrollErrorCode.PAYROLL_VERSION_CONFLICT);
  }
  private PayrollRepository.SnapshotBundle snapshots(Payroll payroll) {
    return payrollRepository.findSnapshots(payroll.getId());
  }
  private PayrollDetailResult detail(Payroll payroll, PayrollRepository.SnapshotBundle snapshots) {
    return PayrollDetailResult.of(payroll, employee(payroll.getUserId()), snapshots,
        statementPort.findByPayrollId(payroll.getId()).orElse(null));
  }
  private BigDecimal sum(List<PayrollListResult.Row> rows,
      Function<PayrollListResult.Row, BigDecimal> getter) {
    return rows.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
  public record ItemAdjustment(Long itemId, BigDecimal amount, String reason) {}
}
