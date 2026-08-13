package com.academy.mudogroupware.payroll.domain.model;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;

class PayrollTest {
  @Test
  void 재계산해도_수기_지급항목은_유지한다() {
    Payroll payroll = Payroll.restore(1L, 10L, YearMonth.of(2026, 8),
        LocalDate.of(2026, 9, 5), PayrollStatus.CALCULATED, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, 1, null, null, LocalDateTime.now(), null, 0,
        List.of(manual("특별수당", "100000"), automatic("기본급", "3000000")));

    payroll.replaceCalculatedItems(List.of(automatic("기본급", "3200000")), LocalDateTime.now());

    assertThat(payroll.getItems()).extracting(PayrollItem::name)
        .containsExactly("기본급", "특별수당");
    assertThat(payroll.getTotalEarnings()).isEqualByComparingTo("3300000");
  }

  @Test
  void 중복_확정은_상태와_확정시각을_바꾸지_않는다() {
    LocalDateTime confirmedAt = LocalDateTime.of(2026, 8, 31, 10, 0);
    Payroll payroll = Payroll.restore(1L, 10L, YearMonth.of(2026, 8),
        LocalDate.of(2026, 9, 5), PayrollStatus.CONFIRMED, new BigDecimal("3000000"),
        BigDecimal.ZERO, new BigDecimal("3000000"), 1, null, null,
        LocalDateTime.now(), confirmedAt, 2, List.of(automatic("기본급", "3000000")));

    boolean changed = payroll.confirm(confirmedAt.plusHours(1));

    assertThat(changed).isFalse();
    assertThat(payroll.getConfirmedAt()).isEqualTo(confirmedAt);
  }

  @Test
  void 초안은_메모를_수정할_수_없다() {
    Payroll payroll = Payroll.draft(10L, YearMonth.of(2026, 8), LocalDate.of(2026, 9, 5));

    assertThatThrownBy(() -> payroll.updateMemo("메모"))
        .isInstanceOf(PayrollException.class);
  }

  @Test
  void 기본_지급항목이_없으면_확정할_수_없다() {
    Payroll payroll = Payroll.restore(1L, 10L, YearMonth.of(2026, 8),
        LocalDate.of(2026, 9, 5), PayrollStatus.CALCULATED, new BigDecimal("100000"),
        BigDecimal.ZERO, new BigDecimal("100000"), 1, null, null,
        LocalDateTime.now(), null, 0, List.of(manual("특별수당", "100000")));

    assertThatThrownBy(() -> payroll.confirm(LocalDateTime.now()))
        .isInstanceOf(PayrollException.class);
  }

  private PayrollItem manual(String name, String amount) {
    return new PayrollItem(1L, ItemCategory.EARNING, ItemType.OTHER_ALLOWANCE, name,
        new BigDecimal(amount), SourceType.MANUAL, null, false, null, null, null, 2);
  }
  private PayrollItem automatic(String name, String amount) {
    return new PayrollItem(2L, ItemCategory.EARNING, ItemType.BASE_SALARY, name,
        new BigDecimal(amount), SourceType.CONTRACT, null, false, null, null, null, 1);
  }
}
