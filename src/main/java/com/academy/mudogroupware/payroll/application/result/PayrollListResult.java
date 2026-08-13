package com.academy.mudogroupware.payroll.application.result;

import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import java.math.BigDecimal;
import java.util.List;

public record PayrollListResult(
    List<Row> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean hasNext,
    boolean hasPrevious,
    Summary summary) {
  public record Row(Long employeeId, String employeeName, EmploymentType employmentType,
      Long payrollId, String preparationStatus, BigDecimal totalEarnings,
      BigDecimal totalDeductions, BigDecimal netPay, int revisionNo) {}
  public record Summary(long targetEmployeeCount, long notCreatedCount, long calculatedCount,
      long confirmedCount, BigDecimal totalEarnings, BigDecimal totalDeductions,
      BigDecimal totalNetPay) {}
}
