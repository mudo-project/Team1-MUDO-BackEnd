package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.global.domain.common.page.PagedResult;
import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.*;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort.StatementData;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollStatementEmailService {
  private final PayrollRepository payrolls;
  private final PayrollStatementPort statements;
  private final PayrollStatementDeliveryPort deliveries;
  private final PayrollEmployeePort employees;
  private final ApplicationEventPublisher events;

  @Transactional
  public DeliveryResult send(Long payrollId, Long requestedBy) {
    Payroll payroll = payroll(payrollId);
    requireConfirmedLatest(payroll);
    StatementData statement = readyStatementForUpdate(payrollId);
    Optional<DeliveryData> existing = deliveries.findBlocking(statement.id());
    if (existing.isPresent()) {
      return DeliveryResult.reused(existing.get());
    }
    var employee = employees.findById(payroll.getUserId())
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_EMPLOYEE_EMAIL_MISSING));
    if (employee.email() == null || employee.email().isBlank()) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_EMPLOYEE_EMAIL_MISSING);
    }
    DeliveryData delivery = create(null, payroll, statement, employee.email(), requestedBy,
        DeliveryStatus.PENDING, null, null);
    events.publishEvent(new PayrollStatementEmailRequestedEvent(delivery.id()));
    return DeliveryResult.created(delivery);
  }

  @Transactional
  public BatchCreatedResult sendBatch(YearMonth month, Long requestedBy) {
    LocalDateTime now = LocalDateTime.now();
    BatchData batch = deliveries.createBatch(month, requestedBy, now);
    List<Payroll> targets = payrolls.findLatestByMonth(month);
    Set<Long> payrollIds = targets.stream().map(Payroll::getId).collect(Collectors.toSet());
    Map<Long, StatementData> statementsByPayroll =
        statements.findByPayrollIdsForUpdate(payrollIds);
    Set<Long> statementIds = statementsByPayroll.values().stream()
        .map(StatementData::id).collect(Collectors.toSet());
    Map<Long, DeliveryData> blockingByStatement =
        deliveries.findBlockingByStatementIds(statementIds);
    Set<Long> userIds = targets.stream().map(Payroll::getUserId).collect(Collectors.toSet());
    Map<Long, PayrollEmployeePort.EmployeeView> employeesById = employees.findByIds(userIds);
    for (Payroll payroll : targets) {
      StatementData statement = statementsByPayroll.get(payroll.getId());
      createBatchDelivery(batch.id(), payroll, statement,
          statement == null ? null : blockingByStatement.get(statement.id()),
          employeesById.get(payroll.getUserId()), requestedBy);
    }
    long total = deliveries.countByBatch(batch.id());
    return new BatchCreatedResult(batch.id(), month.atDay(1), total, batchStatus(batch.id()));
  }

  @Transactional(readOnly = true)
  public BatchResult getBatch(Long batchId, int page, int size) {
    BatchData batch = deliveries.findBatch(batchId)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_EMAIL_BATCH_NOT_FOUND));
    long total = deliveries.countByBatch(batchId);
    int offset = Math.multiplyExact(page, size);
    List<DeliveryData> pageRows = deliveries.findByBatch(batchId, size + 1, offset);
    Set<Long> userIds = pageRows.stream().map(DeliveryData::userId).collect(Collectors.toSet());
    Map<Long, PayrollEmployeePort.EmployeeView> employeesById = employees.findByIds(userIds);
    List<DeliveryView> content = pageRows.stream()
        .map(data -> view(data, employeesById)).toList();
    boolean hasNext = content.size() > size;
    if (hasNext) content = content.subList(0, size);
    Map<DeliveryStatus, Long> counts = counts(batchId);
    return new BatchResult(batch.id(), batch.yearMonth().atDay(1), status(counts, total),
        Summary.from(counts, total), PagedResult.of(content, page, size, total));
  }

  private void createBatchDelivery(Long batchId, Payroll payroll, StatementData statement,
      DeliveryData blocking, PayrollEmployeePort.EmployeeView employee, Long requestedBy) {
    String email = employee == null ? null : employee.email();
    if (payroll.getStatus() != PayrollStatus.CONFIRMED) {
      create(batchId, payroll, statement, email,
          requestedBy, DeliveryStatus.SKIPPED, "PAYROLL_NOT_CONFIRMED", "급여가 확정되지 않았습니다.");
      return;
    }
    if (statement == null || statement.status() != StatementStatus.READY
        || statement.objectKey() == null) {
      create(batchId, payroll, statement, email, requestedBy, DeliveryStatus.SKIPPED,
          "STATEMENT_NOT_READY", "급여명세서가 준비되지 않았습니다.");
      return;
    }
    if (email == null || email.isBlank()) {
      create(batchId, payroll, statement, null, requestedBy, DeliveryStatus.SKIPPED,
          "NO_EMAIL", "직원 이메일이 등록되어 있지 않습니다.");
      return;
    }
    if (blocking != null) {
      create(batchId, payroll, statement, email, requestedBy, DeliveryStatus.SKIPPED,
          "ALREADY_DELIVERED_OR_IN_PROGRESS", "이미 전달됐거나 발송 처리 중입니다.");
      return;
    }
    DeliveryData delivery = create(batchId, payroll, statement, email, requestedBy,
        DeliveryStatus.PENDING, null, null);
    events.publishEvent(new PayrollStatementEmailRequestedEvent(delivery.id()));
  }

  private DeliveryData create(Long batchId, Payroll payroll, StatementData statement, String email,
      Long requestedBy, DeliveryStatus status, String failureCode, String failureReason) {
    return deliveries.create(batchId, payroll.getId(), statement == null ? null : statement.id(),
        payroll.getUserId(), email, UUID.randomUUID().toString(), requestedBy,
        LocalDateTime.now(), status, failureCode, failureReason);
  }

  private Payroll payroll(Long payrollId) {
    return payrolls.findById(payrollId)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND));
  }

  private void requireConfirmedLatest(Payroll payroll) {
    if (payroll.getStatus() != PayrollStatus.CONFIRMED) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_STATE);
    }
    Payroll latest = payrolls.findLatest(payroll.getUserId(), payroll.getYearMonth())
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND));
    if (!latest.getId().equals(payroll.getId())) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_EMAIL_NOT_LATEST_REVISION);
    }
  }

  private StatementData readyStatementForUpdate(Long payrollId) {
    StatementData statement = statements.findByPayrollIdForUpdate(payrollId)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_STATEMENT_NOT_READY));
    if (statement.status() != StatementStatus.READY || statement.objectKey() == null) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_STATEMENT_NOT_READY);
    }
    return statement;
  }

  private DeliveryView view(DeliveryData data,
      Map<Long, PayrollEmployeePort.EmployeeView> employeesById) {
    PayrollEmployeePort.EmployeeView employee = employeesById.get(data.userId());
    String name = employee == null ? "알 수 없음" : employee.name();
    return new DeliveryView(data.id(), data.payrollId(), data.userId(), name,
        mask(data.recipientEmail()), data.status(), data.failureCode(), data.failureReason(),
        data.requestedAt(), data.sentAt(), data.deliveredAt(), data.failedAt());
  }

  private String mask(String email) {
    if (email == null || email.isBlank()) return null;
    int at = email.indexOf('@');
    if (at <= 0) return "***";
    String local = email.substring(0, at);
    String visible = local.substring(0, Math.min(2, local.length()));
    return visible + "***" + email.substring(at);
  }

  private DeliveryBatchStatus batchStatus(Long batchId) {
    long total = deliveries.countByBatch(batchId);
    return status(counts(batchId), total);
  }

  private Map<DeliveryStatus, Long> counts(Long batchId) {
    Map<DeliveryStatus, Long> counts = new EnumMap<>(DeliveryStatus.class);
    for (StatusCount count : deliveries.countStatuses(batchId)) {
      counts.put(count.status(), count.count());
    }
    return counts;
  }

  private DeliveryBatchStatus status(Map<DeliveryStatus, Long> counts, long total) {
    if (total == 0) return DeliveryBatchStatus.COMPLETED;
    long pending = counts.getOrDefault(DeliveryStatus.PENDING, 0L);
    long sending = counts.getOrDefault(DeliveryStatus.SENDING, 0L);
    long retryWait = counts.getOrDefault(DeliveryStatus.RETRY_WAIT, 0L);
    long unknown = counts.getOrDefault(DeliveryStatus.UNKNOWN, 0L);
    long sent = counts.getOrDefault(DeliveryStatus.SENT, 0L);
    if (pending == total) return DeliveryBatchStatus.PENDING;
    if (pending + sending + retryWait > 0) return DeliveryBatchStatus.PROCESSING;
    if (sent + unknown > 0) return DeliveryBatchStatus.AWAITING_DELIVERY;
    return DeliveryBatchStatus.COMPLETED;
  }

  public record DeliveryResult(Long deliveryId, Long payrollId, DeliveryStatus status,
      LocalDateTime requestedAt, boolean reused) {
    static DeliveryResult created(DeliveryData data) {
      return from(data, false);
    }

    static DeliveryResult reused(DeliveryData data) {
      return from(data, true);
    }

    private static DeliveryResult from(DeliveryData data, boolean reused) {
      return new DeliveryResult(data.id(), data.payrollId(), data.status(), data.requestedAt(),
          reused);
    }
  }

  public record BatchCreatedResult(Long batchId, java.time.LocalDate payrollYearMonth,
      long targetCount, DeliveryBatchStatus status) {}

  public record BatchResult(Long batchId, java.time.LocalDate payrollYearMonth,
      DeliveryBatchStatus status, Summary summary, PagedResult<DeliveryView> deliveries) {}

  public record Summary(long totalCount, long pendingCount, long sendingCount, long sentCount,
      long retryWaitCount, long unknownCount, long deliveredCount, long failedCount,
      long skippedCount) {
    static Summary from(Map<DeliveryStatus, Long> counts, long total) {
      return new Summary(total, counts.getOrDefault(DeliveryStatus.PENDING, 0L),
          counts.getOrDefault(DeliveryStatus.SENDING, 0L),
          counts.getOrDefault(DeliveryStatus.SENT, 0L),
          counts.getOrDefault(DeliveryStatus.RETRY_WAIT, 0L),
          counts.getOrDefault(DeliveryStatus.UNKNOWN, 0L),
          counts.getOrDefault(DeliveryStatus.DELIVERED, 0L),
          counts.getOrDefault(DeliveryStatus.FAILED, 0L),
          counts.getOrDefault(DeliveryStatus.SKIPPED, 0L));
    }
  }

  public record DeliveryView(Long deliveryId, Long payrollId, Long employeeId,
      String employeeName, String recipientEmail, DeliveryStatus status, String failureCode,
      String failureReason, LocalDateTime requestedAt, LocalDateTime sentAt,
      LocalDateTime deliveredAt, LocalDateTime failedAt) {}
}
