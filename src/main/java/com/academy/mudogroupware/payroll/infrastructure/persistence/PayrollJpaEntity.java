package com.academy.mudogroupware.payroll.infrastructure.persistence;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payroll")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PayrollJpaEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payroll_id")
  private Long id;
  @Column(name = "user_id", nullable = false) private Long userId;
  @Column(name = "payroll_year_month", nullable = false) private LocalDate yearMonth;
  @Column(name = "scheduled_pay_date", nullable = false) private LocalDate scheduledPayDate;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private PayrollStatus status;
  @Column(name = "total_earnings") private BigDecimal totalEarnings;
  @Column(name = "total_deductions") private BigDecimal totalDeductions;
  @Column(name = "net_pay") private BigDecimal netPay;
  @Column(name = "revision_no", nullable = false) private int revisionNo;
  @Column(name = "original_payroll_id") private Long originalPayrollId;
  @Column(length = 1000) private String memo;
  @Column(name = "calculated_at") private LocalDateTime calculatedAt;
  @Column(name = "confirmed_at") private LocalDateTime confirmedAt;
  @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
  @Version @Column(nullable = false) private long version;

  @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder asc")
  private List<PayrollItemJpaEntity> items = new ArrayList<>();

  static PayrollJpaEntity create(Long userId, LocalDate yearMonth, LocalDate scheduledPayDate,
      PayrollStatus status, BigDecimal totalEarnings, BigDecimal totalDeductions, BigDecimal netPay,
      int revisionNo, Long originalPayrollId, String memo,
      LocalDateTime calculatedAt, LocalDateTime confirmedAt) {
    PayrollJpaEntity entity = new PayrollJpaEntity();
    entity.userId = userId;
    entity.yearMonth = yearMonth;
    entity.scheduledPayDate = scheduledPayDate;
    entity.status = status;
    entity.totalEarnings = totalEarnings;
    entity.totalDeductions = totalDeductions;
    entity.netPay = netPay;
    entity.revisionNo = revisionNo;
    entity.originalPayrollId = originalPayrollId;
    entity.memo = memo;
    entity.calculatedAt = calculatedAt;
    entity.confirmedAt = confirmedAt;
    entity.createdAt = LocalDateTime.now();
    entity.updatedAt = entity.createdAt;
    return entity;
  }

  void apply(PayrollStatus status, BigDecimal earnings, BigDecimal deductions, BigDecimal netPay,
      String memo, LocalDateTime calculatedAt, LocalDateTime confirmedAt) {
    this.status = status;
    this.totalEarnings = earnings;
    this.totalDeductions = deductions;
    this.netPay = netPay;
    this.memo = memo;
    this.calculatedAt = calculatedAt;
    this.confirmedAt = confirmedAt;
    this.updatedAt = LocalDateTime.now();
  }

  void replaceItems(List<com.academy.mudogroupware.payroll.domain.model.PayrollItem> source) {
    Map<Long, PayrollItemJpaEntity> existing = items.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(PayrollItemJpaEntity::getId, Function.identity()));
    List<PayrollItemJpaEntity> synchronizedItems = new ArrayList<>();
    for (com.academy.mudogroupware.payroll.domain.model.PayrollItem item : source) {
      PayrollItemJpaEntity entity = item.id() == null ? null : existing.get(item.id());
      if (entity == null) {
        entity = PayrollItemJpaEntity.from(this, item);
      } else {
        entity.apply(item);
      }
      synchronizedItems.add(entity);
    }
    items.removeIf(item -> !synchronizedItems.contains(item));
    for (PayrollItemJpaEntity item : synchronizedItems) {
      if (!items.contains(item)) items.add(item);
    }
  }
}

@Entity
@Table(name = "payroll_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PayrollItemJpaEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payroll_item_id") private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payroll_id") private PayrollJpaEntity payroll;
  @Enumerated(EnumType.STRING) @Column(name = "item_category") private ItemCategory category;
  @Enumerated(EnumType.STRING) @Column(name = "item_type") private ItemType type;
  @Column(name = "item_name") private String name;
  private BigDecimal amount;
  @Enumerated(EnumType.STRING) @Column(name = "source_type") private SourceType sourceType;
  @Column(name = "original_amount") private BigDecimal originalAmount;
  @Column(name = "is_adjusted") private boolean adjusted;
  @Column(name = "adjustment_reason") private String adjustmentReason;
  @Column(name = "calculation_formula") private String calculationFormula;
  @Column(name = "calculation_basis", columnDefinition = "json") private String calculationBasis;
  @Column(name = "display_order") private int displayOrder;
  @Column(name = "created_at") private LocalDateTime createdAt;
  @Column(name = "updated_at") private LocalDateTime updatedAt;

  static PayrollItemJpaEntity from(PayrollJpaEntity payroll,
      com.academy.mudogroupware.payroll.domain.model.PayrollItem item) {
    PayrollItemJpaEntity entity = new PayrollItemJpaEntity();
    entity.payroll = payroll;
    entity.category = item.category();
    entity.type = item.type();
    entity.name = item.name();
    entity.amount = item.amount();
    entity.sourceType = item.sourceType();
    entity.originalAmount = item.originalAmount();
    entity.adjusted = item.adjusted();
    entity.adjustmentReason = item.adjustmentReason();
    entity.calculationFormula = item.calculationFormula();
    entity.calculationBasis = item.calculationBasis();
    entity.displayOrder = item.displayOrder();
    entity.createdAt = LocalDateTime.now();
    entity.updatedAt = entity.createdAt;
    return entity;
  }

  void apply(com.academy.mudogroupware.payroll.domain.model.PayrollItem item) {
    this.category = item.category();
    this.type = item.type();
    this.name = item.name();
    this.amount = item.amount();
    this.sourceType = item.sourceType();
    this.originalAmount = item.originalAmount();
    this.adjusted = item.adjusted();
    this.adjustmentReason = item.adjustmentReason();
    this.calculationFormula = item.calculationFormula();
    this.calculationBasis = item.calculationBasis();
    this.displayOrder = item.displayOrder();
    this.updatedAt = LocalDateTime.now();
  }
}
