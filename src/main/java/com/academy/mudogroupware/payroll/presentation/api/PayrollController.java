package com.academy.mudogroupware.payroll.presentation.api;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.*;
import com.academy.mudogroupware.payroll.application.result.*;
import com.academy.mudogroupware.payroll.application.service.PayrollService;
import com.academy.mudogroupware.payroll.application.service.PayrollService.ItemAdjustment;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PAYROLL:MANAGE')")
public class PayrollController {
  private final PayrollService service;
  private final PayrollStatementService statementService;

  @GetMapping("/api/payrolls")
  public GlobalApiResponse<PayrollListResult> list(
      @RequestParam int year, @RequestParam int month,
      @RequestParam(required = false) EmploymentType employmentType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String employeeName,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return GlobalApiResponse.ok("PAYROLL_LIST_RETRIEVED", "급여 목록을 조회했습니다.",
        service.list(YearMonth.of(year, month), employmentType, status, employeeName, page, size));
  }

  @PostMapping("/api/payrolls/employees/{employeeId}")
  public GlobalApiResponse<PayrollDetailResult> create(@PathVariable Long employeeId,
      @Valid @RequestBody CreateRequest request) {
    return GlobalApiResponse.created("PAYROLL_CREATED", "급여 초안을 생성했습니다.",
        service.create(employeeId, YearMonth.of(request.year(), request.month())));
  }

  @PatchMapping("/api/payrolls/{payrollId}/calculate")
  public GlobalApiResponse<PayrollDetailResult> calculate(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.ok("PAYROLL_CALCULATED", "급여를 계산했습니다.",
        service.calculate(payrollId, request.expectedVersion()));
  }

  @GetMapping("/api/payrolls/{payrollId}")
  public GlobalApiResponse<PayrollDetailResult> get(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok("PAYROLL_RETRIEVED", "급여를 조회했습니다.", service.get(payrollId));
  }

  @PatchMapping("/api/payrolls/{payrollId}")
  public GlobalApiResponse<PayrollDetailResult> update(@PathVariable Long payrollId,
      @Valid @RequestBody UpdateRequest request) {
    List<ItemAdjustment> adjustments = request.adjustments() == null ? List.of()
        : request.adjustments().stream().map(a -> new ItemAdjustment(a.itemId(), a.amount(), a.reason())).toList();
    return GlobalApiResponse.ok("PAYROLL_UPDATED", "급여를 수정했습니다.",
        service.update(payrollId, request.expectedVersion(), request.memo(), adjustments));
  }

  @PostMapping("/api/payrolls/{payrollId}/earnings")
  public GlobalApiResponse<PayrollDetailResult> addEarning(@PathVariable Long payrollId,
      @Valid @RequestBody EarningRequest request) {
    return GlobalApiResponse.created("PAYROLL_EARNING_ADDED", "지급항목을 추가했습니다.",
        service.addEarning(payrollId, request.expectedVersion(), request.name(), request.amount()));
  }

  @DeleteMapping("/api/payrolls/{payrollId}/earnings/{itemId}")
  public GlobalApiResponse<PayrollDetailResult> deleteEarning(@PathVariable Long payrollId,
      @PathVariable Long itemId, @RequestParam long expectedVersion) {
    return GlobalApiResponse.ok("PAYROLL_EARNING_DELETED", "지급항목을 삭제했습니다.",
        service.deleteEarning(payrollId, itemId, expectedVersion));
  }

  @PatchMapping("/api/payrolls/{payrollId}/confirm")
  public GlobalApiResponse<PayrollDetailResult> confirm(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.ok("PAYROLL_CONFIRMED", "급여를 확정했습니다.",
        service.confirm(payrollId, request.expectedVersion()));
  }

  @PostMapping("/api/payrolls/{payrollId}/revisions")
  public GlobalApiResponse<PayrollDetailResult> revision(@PathVariable Long payrollId,
      @Valid @RequestBody VersionRequest request) {
    return GlobalApiResponse.created("PAYROLL_REVISION_CREATED", "급여 정정본을 생성했습니다.",
        service.createRevision(payrollId, request.expectedVersion()));
  }

  @GetMapping("/api/payrolls/{payrollId}/revisions")
  public GlobalApiResponse<List<PayrollDetailResult>> revisions(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok("PAYROLL_REVISIONS_RETRIEVED", "급여 정정 이력을 조회했습니다.",
        service.revisions(payrollId));
  }

  @GetMapping("/api/payrolls/{payrollId}/preview")
  public GlobalApiResponse<PayrollDetailResult> preview(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok("PAYROLL_PREVIEW_RETRIEVED", "급여명세서 미리보기를 조회했습니다.",
        service.preview(payrollId));
  }

  @GetMapping("/api/payrolls/{payrollId}/statement/download-url")
  public GlobalApiResponse<PayrollStatementService.DownloadResult> download(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok("PAYROLL_STATEMENT_URL_ISSUED", "급여명세서 다운로드 URL을 발급했습니다.",
        statementService.download(payrollId));
  }

  @PatchMapping("/api/payrolls/{payrollId}/statement/retry")
  public GlobalApiResponse<PayrollStatementService.StatementResult> retry(@PathVariable Long payrollId) {
    return GlobalApiResponse.ok("PAYROLL_STATEMENT_RETRY_STARTED", "급여명세서 생성을 재시도합니다.",
        statementService.retry(payrollId));
  }

  @GetMapping("/api/payroll/policies")
  public GlobalApiResponse<PayrollPolicyData> getPolicy() {
    return GlobalApiResponse.ok("PAYROLL_POLICY_RETRIEVED", "급여 정책을 조회했습니다.", service.getPolicy());
  }

  @PatchMapping("/api/payroll/policies")
  public GlobalApiResponse<PayrollPolicyData> updatePolicy(@Valid @RequestBody PolicyRequest request) {
    return GlobalApiResponse.ok("PAYROLL_POLICY_UPDATED", "급여 정책을 수정했습니다.",
        service.updatePolicy(new PayrollPolicyData(null, request.payDayType(), request.payDay(),
            request.paymentMonthOffset())));
  }

  @GetMapping("/api/payroll/employees/{employeeId}/compensation")
  public GlobalApiResponse<List<CompensationData>> compensations(@PathVariable Long employeeId) {
    return GlobalApiResponse.ok("PAYROLL_COMPENSATION_RETRIEVED", "직원 급여 계약을 조회했습니다.",
        service.compensations(employeeId));
  }

  @PatchMapping("/api/payroll/employees/{employeeId}/compensation")
  public GlobalApiResponse<CompensationData> compensation(@PathVariable Long employeeId,
      @Valid @RequestBody CompensationRequest request) {
    CompensationData data = new CompensationData(request.compensationId(), employeeId,
        request.employmentType(), request.salaryType(), request.baseSalary(), request.hourlyWage(),
        request.weeklyContractHours(), request.effectiveFrom(), request.effectiveTo());
    return GlobalApiResponse.ok("PAYROLL_COMPENSATION_UPDATED", "직원 급여 계약을 저장했습니다.",
        service.saveCompensation(employeeId, data));
  }

  public record CreateRequest(@Min(2000) int year, @Min(1) @Max(12) int month) {}
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
      @NotNull LocalDate effectiveFrom, LocalDate effectiveTo) {}
}
