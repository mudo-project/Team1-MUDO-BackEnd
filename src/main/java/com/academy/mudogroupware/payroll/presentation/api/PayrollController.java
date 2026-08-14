package com.academy.mudogroupware.payroll.presentation.api;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import static com.academy.mudogroupware.payroll.presentation.api.common.PayrollResponseCode.*;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.*;
import com.academy.mudogroupware.payroll.application.result.*;
import com.academy.mudogroupware.payroll.application.service.PayrollService;
import com.academy.mudogroupware.payroll.application.service.PayrollService.ItemAdjustment;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PAYROLL:MANAGE')")
@Tag(name = "급여", description = "급여 정책·직원 급여 설정·월 급여·급여명세서 관리 API")
@Validated
public class PayrollController {
  private final PayrollService service;
  private final PayrollStatementService statementService;
  private final PayrollStatementEmailService statementEmailService;

  @GetMapping("/api/payrolls")
  @Operation(summary = "월 급여 목록 조회", description = "급여가 없는 활성 직원도 NOT_CREATED 상태로 포함해 페이지 조회합니다.")
  public GlobalApiResponse<PayrollListResult> list(
      @RequestParam int year, @RequestParam int month,
      @RequestParam(required = false) EmploymentType employmentType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String employeeName,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return GlobalApiResponse.ok(LIST_RETRIEVED,
        service.list(YearMonth.of(year, month), employmentType, status, employeeName, page, size));
  }

  @PostMapping("/api/payrolls/employees/{employeeId}")
  @Operation(summary = "급여 초안 생성")
  public GlobalApiResponse<PayrollDetailResult> create(@PathVariable Long employeeId,
      @Valid @RequestBody CreateRequest request) {
    return GlobalApiResponse.created(CREATED,
        service.create(employeeId, YearMonth.of(request.year(), request.month())));
  }

  @PatchMapping("/api/payrolls/{payrollId}/calculate")
  @Operation(summary = "급여 계산 또는 재계산", description = "MANUAL 지급항목은 유지하고 자동 항목과 Snapshot을 교체합니다.")
  public GlobalApiResponse<PayrollDetailResult> calculate(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.ok(CALCULATED,
        service.calculate(payrollId, request.expectedVersion()));
  }

  @GetMapping("/api/payrolls/{payrollId}")
  @Operation(summary = "급여 상세 조회")
  public GlobalApiResponse<PayrollDetailResult> get(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok(RETRIEVED, service.get(payrollId));
  }

  @PatchMapping("/api/payrolls/{payrollId}")
  @Operation(summary = "급여 지급항목·메모 수정", description = "확정 후에는 메모만 수정할 수 있습니다.")
  public GlobalApiResponse<PayrollDetailResult> update(@PathVariable Long payrollId,
      @Valid @RequestBody UpdateRequest request) {
    List<ItemAdjustment> adjustments = request.adjustments() == null ? List.of()
        : request.adjustments().stream().map(a -> new ItemAdjustment(a.itemId(), a.amount(), a.reason())).toList();
    return GlobalApiResponse.ok(UPDATED,
        service.update(payrollId, request.expectedVersion(), request.memo(), adjustments));
  }

  @PostMapping("/api/payrolls/{payrollId}/earnings")
  @Operation(summary = "수기 지급항목 추가")
  public GlobalApiResponse<PayrollDetailResult> addEarning(@PathVariable Long payrollId,
      @Valid @RequestBody EarningRequest request) {
    return GlobalApiResponse.created(EARNING_ADDED,
        service.addEarning(payrollId, request.expectedVersion(), request.name(), request.amount()));
  }

  @DeleteMapping("/api/payrolls/{payrollId}/earnings/{itemId}")
  @Operation(summary = "수기 지급항목 삭제")
  public GlobalApiResponse<PayrollDetailResult> deleteEarning(@PathVariable Long payrollId,
      @PathVariable Long itemId, @RequestParam long expectedVersion) {
    return GlobalApiResponse.ok(EARNING_DELETED,
        service.deleteEarning(payrollId, itemId, expectedVersion));
  }

  @PatchMapping("/api/payrolls/{payrollId}/confirm")
  @Operation(summary = "급여 확정", description = "멱등 처리하며 커밋 후 PDF 명세서 생성을 시작합니다.")
  public GlobalApiResponse<PayrollDetailResult> confirm(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.ok(CONFIRMED,
        service.confirm(payrollId, request.expectedVersion()));
  }

  @PostMapping("/api/payrolls/{payrollId}/revisions")
  @Operation(summary = "급여 정정본 생성")
  public GlobalApiResponse<PayrollDetailResult> revision(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.created(REVISION_CREATED,
        service.createRevision(payrollId, request.expectedVersion()));
  }

  @GetMapping("/api/payrolls/{payrollId}/revisions")
  @Operation(summary = "급여 정정 이력 조회")
  public GlobalApiResponse<List<PayrollDetailResult>> revisions(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok(REVISIONS_RETRIEVED,
        service.revisions(payrollId));
  }

  @GetMapping("/api/payrolls/{payrollId}/preview")
  @Operation(summary = "급여명세서 JSON 미리보기")
  public GlobalApiResponse<PayrollDetailResult> preview(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok(PREVIEW_RETRIEVED,
        service.preview(payrollId));
  }

  @GetMapping("/api/payrolls/{payrollId}/statement/download-url")
  @Operation(summary = "확정 급여명세서 다운로드 URL 발급")
  public GlobalApiResponse<PayrollStatementService.DownloadResult> download(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok(STATEMENT_URL_ISSUED,
        statementService.download(payrollId));
  }

  @PatchMapping("/api/payrolls/{payrollId}/statement/retry")
  @Operation(summary = "실패한 급여명세서 생성 재시도")
  public GlobalApiResponse<PayrollStatementService.StatementResult> retry(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok(STATEMENT_RETRY_STARTED,
        statementService.retry(payrollId));
  }

  @PostMapping("/api/payrolls/{payrollId}/statement/email-deliveries")
  @Operation(summary = "급여명세서 개별 이메일 발송")
  public ResponseEntity<GlobalApiResponse<PayrollStatementEmailService.DeliveryResult>> sendStatementEmail(
      @AuthenticationPrincipal AuthUser authUser, @PathVariable Long payrollId) {
    PayrollStatementEmailService.DeliveryResult result =
        statementEmailService.send(payrollId, authUser.userId());
    if (result.reused()) {
      return ResponseEntity.ok(GlobalApiResponse.ok(STATEMENT_EMAIL_DELIVERY_REUSED, result));
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(GlobalApiResponse.created(STATEMENT_EMAIL_SEND_STARTED, result));
  }

  @PostMapping("/api/payrolls/statement/email-delivery-batches")
  @Operation(summary = "급여명세서 이메일 일괄 발송")
  public GlobalApiResponse<PayrollStatementEmailService.BatchCreatedResult> sendStatementEmailBatch(
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody EmailBatchRequest request) {
    return GlobalApiResponse.created(STATEMENT_EMAIL_BATCH_STARTED,
        statementEmailService.sendBatch(YearMonth.of(request.year(), request.month()),
            authUser.userId()));
  }

  @GetMapping("/api/payrolls/statement/email-delivery-batches/{batchId}")
  @Operation(summary = "급여명세서 이메일 일괄 발송 결과 조회")
  public GlobalApiResponse<PayrollStatementEmailService.BatchResult> getStatementEmailBatch(
      @PathVariable Long batchId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return GlobalApiResponse.ok(STATEMENT_EMAIL_BATCH_RETRIEVED,
        statementEmailService.getBatch(batchId, page, size));
  }

  @GetMapping("/api/payroll/policies")
  @Operation(summary = "급여 정책 조회")
  public GlobalApiResponse<PayrollPolicyData> getPolicy() {
    return GlobalApiResponse.ok(POLICY_RETRIEVED, service.getPolicy());
  }

  @PatchMapping("/api/payroll/policies")
  @Operation(summary = "급여 정책 수정")
  public GlobalApiResponse<PayrollPolicyData> updatePolicy(@Valid @RequestBody PolicyRequest request) {
    return GlobalApiResponse.ok(POLICY_UPDATED,
        service.updatePolicy(new PayrollPolicyData(null, request.payDayType(), request.payDay(),
            request.paymentMonthOffset())));
  }

  @GetMapping("/api/payroll/employees/{employeeId}/compensation")
  @Operation(summary = "직원 급여 설정 조회", description = "계약·고정수당·통상시급 적용 이력을 조회합니다.")
  public GlobalApiResponse<EmployeePayrollSettingsResult> compensations(@PathVariable Long employeeId) {
    return GlobalApiResponse.ok(SETTINGS_RETRIEVED,
        service.employeeSettings(employeeId));
  }

  @PatchMapping("/api/payroll/employees/{employeeId}/compensation")
  @Operation(summary = "직원 급여 설정 저장", description = "계약·고정수당·통상시급을 등록하거나 수정합니다.")
  public GlobalApiResponse<EmployeePayrollSettingsResult> compensation(@PathVariable Long employeeId,
      @Valid @RequestBody EmployeeSettingsRequest request) {
    CompensationData compensation = request.compensation() == null ? null
        : request.compensation().toData(employeeId);
    List<AllowanceData> allowances = request.fixedAllowances() == null ? null
        : request.fixedAllowances().stream().map(item -> item.toData(employeeId)).toList();
    PayBasisData payBasis = request.payBasis() == null ? null : request.payBasis().toData(employeeId);
    return GlobalApiResponse.ok(SETTINGS_UPDATED,
        service.saveEmployeeSettings(employeeId, compensation, allowances, payBasis));
  }

  public record CreateRequest(@Min(2000) int year, @Min(1) @Max(12) int month) {}
  public record EmailBatchRequest(@Min(2000) int year, @Min(1) @Max(12) int month) {}
  public record VersionRequest(@Min(0) long expectedVersion) {}
  public record AdjustmentRequest(@NotNull Long itemId, @NotNull @PositiveOrZero BigDecimal amount,
      @NotBlank String reason) {}
  public record UpdateRequest(@Min(0) long expectedVersion, @Size(max = 1000) String memo,
      List<@Valid AdjustmentRequest> adjustments) {}
  public record EarningRequest(@Min(0) long expectedVersion, @NotBlank @Size(max = 100) String name,
      @NotNull @PositiveOrZero BigDecimal amount) {}
  public record PolicyRequest(@NotNull PayDayType payDayType, @Min(1) @Max(31) Integer payDay,
      @Min(0) @Max(12) int paymentMonthOffset) {}
  public record CompensationRequest(Long compensationId, @NotNull EmploymentType employmentType,
      @NotNull SalaryType salaryType, @PositiveOrZero BigDecimal baseSalary,
      @PositiveOrZero BigDecimal hourlyWage, @NotNull @PositiveOrZero BigDecimal weeklyContractHours,
      @NotNull LocalDate effectiveFrom, LocalDate effectiveTo) {
    CompensationData toData(Long employeeId) {
      return new CompensationData(compensationId, employeeId, employmentType, salaryType,
          baseSalary, hourlyWage, weeklyContractHours, effectiveFrom, effectiveTo);
    }
  }
  public record AllowanceRequest(Long allowanceId, @NotBlank String allowanceType,
      @NotBlank @Size(max = 100) String allowanceName,
      @NotNull @PositiveOrZero BigDecimal amount, @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {
    AllowanceData toData(Long employeeId) {
      return new AllowanceData(allowanceId, employeeId, allowanceType, allowanceName,
          amount, effectiveFrom, effectiveTo);
    }
  }
  public record PayBasisRequest(Long payBasisId, @NotNull @Positive BigDecimal ordinaryHourlyWage,
      @NotNull LocalDate effectiveFrom, LocalDate effectiveTo) {
    PayBasisData toData(Long employeeId) {
      return new PayBasisData(payBasisId, employeeId, ordinaryHourlyWage, effectiveFrom, effectiveTo);
    }
  }
  public record EmployeeSettingsRequest(@Valid CompensationRequest compensation,
      List<@Valid AllowanceRequest> fixedAllowances, @Valid PayBasisRequest payBasis) {}
}
