package com.academy.mudogroupware.payroll.application.result;

import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort.EmployeeView;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository.SnapshotBundle;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import com.academy.mudogroupware.payroll.domain.model.PayrollItem;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record PayrollDetailResult(
    Long payrollId,
    Employee employee,
    YearMonth yearMonth,
    LocalDate scheduledPayDate,
    PayrollStatus status,
    int revisionNo,
    Long originalPayrollId,
    SnapshotBundle snapshots,
    List<Item> earnings,
    List<Item> deductions,
    BigDecimal totalEarnings,
    BigDecimal totalDeductions,
    BigDecimal netPay,
    String memo,
    long version) {

  public static PayrollDetailResult of(Payroll payroll, EmployeeView employee, SnapshotBundle snapshots) {
    List<Item> earnings = payroll.getItems().stream()
        .filter(i -> i.category() == ItemCategory.EARNING).map(i -> Item.from(i, payroll)).toList();
    List<Item> deductions = payroll.getItems().stream()
        .filter(i -> i.category() == ItemCategory.DEDUCTION).map(i -> Item.from(i, payroll)).toList();
    EmploymentType employmentType = snapshots == null || snapshots.compensations().isEmpty()
        ? null : snapshots.compensations().get(0).employmentType();
    return new PayrollDetailResult(payroll.getId(),
        new Employee(employee.id(), employee.name(), employmentType), payroll.getYearMonth(),
        payroll.getScheduledPayDate(), payroll.getStatus(), payroll.getRevisionNo(),
        payroll.getOriginalPayrollId(), snapshots, earnings, deductions, payroll.getTotalEarnings(),
        payroll.getTotalDeductions(), payroll.getNetPay(), payroll.getMemo(), payroll.getVersion());
  }

  public record Employee(Long employeeId, String name, EmploymentType employmentType) {}
  public record Item(Long itemId, ItemType type, String name, SourceType sourceType,
      BigDecimal amount, BigDecimal originalAmount, boolean adjusted, String adjustmentReason,
      String calculationFormula, boolean editable) {
    static Item from(PayrollItem item, Payroll payroll) {
      boolean editable = payroll.getStatus() == PayrollStatus.CALCULATED
          && item.category() == ItemCategory.EARNING;
      return new Item(item.id(), item.type(), item.name(), item.sourceType(), item.amount(),
          item.originalAmount(), item.adjusted(), item.adjustmentReason(),
          item.calculationFormula(), editable);
    }
  }
}
