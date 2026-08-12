package com.academy.mudogroupware.payroll.domain.model;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class Payroll {
  private final Long id;
  private final Long userId;
  private final YearMonth yearMonth;
  private final LocalDate scheduledPayDate;
  private PayrollStatus status;
  private BigDecimal totalEarnings;
  private BigDecimal totalDeductions;
  private BigDecimal netPay;
  private final int revisionNo;
  private final Long originalPayrollId;
  private String memo;
  private LocalDateTime calculatedAt;
  private LocalDateTime confirmedAt;
  private final long version;
  private final List<PayrollItem> items;

  private Payroll(Long id, Long userId, YearMonth yearMonth, LocalDate scheduledPayDate,
      PayrollStatus status, BigDecimal totalEarnings, BigDecimal totalDeductions,
      BigDecimal netPay, int revisionNo, Long originalPayrollId, String memo,
      LocalDateTime calculatedAt, LocalDateTime confirmedAt, long version,
      List<PayrollItem> items) {
    this.id = id;
    this.userId = userId;
    this.yearMonth = yearMonth;
    this.scheduledPayDate = scheduledPayDate;
    this.status = status;
    this.totalEarnings = totalEarnings;
    this.totalDeductions = totalDeductions;
    this.netPay = netPay;
    this.revisionNo = revisionNo;
    this.originalPayrollId = originalPayrollId;
    this.memo = memo;
    this.calculatedAt = calculatedAt;
    this.confirmedAt = confirmedAt;
    this.version = version;
    this.items = new ArrayList<>(items);
  }

  public static Payroll draft(Long userId, YearMonth yearMonth, LocalDate scheduledPayDate) {
    return new Payroll(null, userId, yearMonth, scheduledPayDate, PayrollStatus.DRAFT,
        null, null, null, 1, null, null, null, null, 0, List.of());
  }

  public static Payroll restore(Long id, Long userId, YearMonth yearMonth,
      LocalDate scheduledPayDate, PayrollStatus status, BigDecimal totalEarnings,
      BigDecimal totalDeductions, BigDecimal netPay, int revisionNo, Long originalPayrollId,
      String memo, LocalDateTime calculatedAt, LocalDateTime confirmedAt, long version,
      List<PayrollItem> items) {
    return new Payroll(id, userId, yearMonth, scheduledPayDate, status, totalEarnings,
        totalDeductions, netPay, revisionNo, originalPayrollId, memo, calculatedAt,
        confirmedAt, version, items);
  }

  public Payroll revision() {
    require(PayrollStatus.CONFIRMED);
    Long originalId = originalPayrollId == null ? id : originalPayrollId;
    return new Payroll(null, userId, yearMonth, scheduledPayDate, PayrollStatus.CALCULATED,
        totalEarnings, totalDeductions, netPay, revisionNo + 1, originalId, memo,
        LocalDateTime.now(), null, 0, items);
  }

  public void replaceCalculatedItems(List<PayrollItem> calculatedItems, LocalDateTime now) {
    if (status == PayrollStatus.CONFIRMED) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_STATE);
    }
    List<PayrollItem> manual = items.stream().filter(i -> i.sourceType() == SourceType.MANUAL).toList();
    items.clear();
    items.addAll(calculatedItems);
    items.addAll(manual);
    status = PayrollStatus.CALCULATED;
    calculatedAt = now;
    recalculateTotals();
  }

  public void adjustItem(Long itemId, BigDecimal amount, String reason) {
    require(PayrollStatus.CALCULATED);
    int index = findItemIndex(itemId);
    items.set(index, items.get(index).adjust(amount, reason));
    recalculateTotals();
  }

  public void addManualEarning(String name, BigDecimal amount) {
    require(PayrollStatus.CALCULATED);
    items.add(new PayrollItem(null, ItemCategory.EARNING, ItemType.OTHER_ALLOWANCE, name,
        amount, SourceType.MANUAL, null, false, null, null, null, items.size() + 1));
    recalculateTotals();
  }

  public void removeManualEarning(Long itemId) {
    require(PayrollStatus.CALCULATED);
    int index = findItemIndex(itemId);
    if (items.get(index).sourceType() != SourceType.MANUAL) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_ITEM_NOT_EDITABLE);
    }
    items.remove(index);
    recalculateTotals();
  }

  public boolean confirm(LocalDateTime now) {
    if (status == PayrollStatus.CONFIRMED) return false;
    require(PayrollStatus.CALCULATED);
    if (items.isEmpty() || totalEarnings == null || totalDeductions == null || netPay == null) {
      throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_STATE);
    }
    status = PayrollStatus.CONFIRMED;
    confirmedAt = now;
    return true;
  }

  public void updateMemo(String memo) {
    this.memo = memo == null || memo.isBlank() ? null : memo.trim();
  }

  private int findItemIndex(Long itemId) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).id() != null && items.get(i).id().equals(itemId)) return i;
    }
    throw new PayrollException(PayrollErrorCode.PAYROLL_ITEM_NOT_EDITABLE);
  }

  private void recalculateTotals() {
    totalEarnings = sum(ItemCategory.EARNING);
    totalDeductions = sum(ItemCategory.DEDUCTION);
    netPay = totalEarnings.subtract(totalDeductions);
  }

  private BigDecimal sum(ItemCategory category) {
    return items.stream().filter(i -> i.category() == category).map(PayrollItem::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void require(PayrollStatus expected) {
    if (status != expected) throw new PayrollException(PayrollErrorCode.INVALID_PAYROLL_STATE);
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public YearMonth getYearMonth() { return yearMonth; }
  public LocalDate getScheduledPayDate() { return scheduledPayDate; }
  public PayrollStatus getStatus() { return status; }
  public BigDecimal getTotalEarnings() { return totalEarnings; }
  public BigDecimal getTotalDeductions() { return totalDeductions; }
  public BigDecimal getNetPay() { return netPay; }
  public int getRevisionNo() { return revisionNo; }
  public Long getOriginalPayrollId() { return originalPayrollId; }
  public String getMemo() { return memo; }
  public LocalDateTime getCalculatedAt() { return calculatedAt; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public long getVersion() { return version; }
  public List<PayrollItem> getItems() { return List.copyOf(items); }
}
