package com.academy.mudogroupware.payroll.domain.model;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import java.math.BigDecimal;

public record PayrollItem(
    Long id,
    ItemCategory category,
    ItemType type,
    String name,
    BigDecimal amount,
    SourceType sourceType,
    BigDecimal originalAmount,
    boolean adjusted,
    String adjustmentReason,
    String calculationFormula,
    String calculationBasis,
    int displayOrder) {

  public PayrollItem {
    if (category == null || type == null || sourceType == null || name == null || name.isBlank()
        || amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("급여 항목 값이 올바르지 않습니다.");
    }
  }

  public PayrollItem adjust(BigDecimal changedAmount, String reason) {
    if (category != ItemCategory.EARNING || sourceType == SourceType.MOCK_INSURANCE
        || sourceType == SourceType.MOCK_TAX || changedAmount == null || changedAmount.signum() < 0
        || reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("지급항목 조정 값이 올바르지 않습니다.");
    }
    BigDecimal origin = adjusted ? originalAmount : amount;
    return new PayrollItem(id, category, type, name, changedAmount, sourceType, origin, true,
        reason.trim(), calculationFormula, calculationBasis, displayOrder);
  }
}
