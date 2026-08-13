package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus.PENDING;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.PayrollStatus.CONFIRMED;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.StatementStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmployeePort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.BatchData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort.StatementData;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailServiceTest {
  @Mock PayrollRepository payrolls;
  @Mock PayrollStatementPort statements;
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock PayrollEmployeePort employees;
  @Mock ApplicationEventPublisher events;
  @InjectMocks PayrollStatementEmailService service;

  @Test
  void 확정된_최신_급여명세서의_개별_발송을_등록한다() {
    Payroll payroll = confirmedPayroll();
    StatementData statement = readyStatement();
    DeliveryData delivery = delivery();
    when(payrolls.findById(1L)).thenReturn(Optional.of(payroll));
    when(payrolls.findLatest(10L, YearMonth.of(2026, 8))).thenReturn(Optional.of(payroll));
    when(statements.findByPayrollIdForUpdate(1L)).thenReturn(Optional.of(statement));
    when(employees.findById(10L)).thenReturn(Optional.of(
        new PayrollEmployeePort.EmployeeView(10L, "직원", "staff@example.com",
            LocalDate.of(2026, 1, 1))));
    when(deliveries.create(isNull(), eq(1L), eq(20L), eq(10L), eq("staff@example.com"),
        anyString(), eq(99L), any(LocalDateTime.class), eq(PENDING), isNull(), isNull()))
        .thenReturn(delivery);

    var result = service.send(1L, 99L);

    assertThat(result.deliveryId()).isEqualTo(30L);
    assertThat(result.status()).isEqualTo(PENDING);
    verify(events).publishEvent(new PayrollStatementEmailRequestedEvent(30L));
  }

  @Test
  void 직원_이메일이_없으면_발송_이력을_만들지_않는다() {
    Payroll payroll = confirmedPayroll();
    when(payrolls.findById(1L)).thenReturn(Optional.of(payroll));
    when(payrolls.findLatest(10L, YearMonth.of(2026, 8))).thenReturn(Optional.of(payroll));
    when(statements.findByPayrollIdForUpdate(1L)).thenReturn(Optional.of(readyStatement()));
    when(employees.findById(10L)).thenReturn(Optional.of(
        new PayrollEmployeePort.EmployeeView(10L, "직원", null, LocalDate.of(2026, 1, 1))));

    assertThatThrownBy(() -> service.send(1L, 99L)).isInstanceOf(PayrollException.class);
    verify(deliveries, never()).create(any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any());
  }

  @Test
  void 일괄_발송_결과에_전체_페이지_정보를_반환한다() {
    when(deliveries.findBatch(30L)).thenReturn(Optional.of(
        new BatchData(30L, YearMonth.of(2026, 8), 99L, LocalDateTime.now())));
    when(deliveries.countByBatch(30L)).thenReturn(42L);
    when(deliveries.findByBatch(30L, 21, 20)).thenReturn(List.of());
    when(deliveries.countStatuses(30L)).thenReturn(List.of());

    var result = service.getBatch(30L, 1, 20);

    assertThat(result.deliveries().totalElements()).isEqualTo(42);
    assertThat(result.deliveries().totalPages()).isEqualTo(3);
    assertThat(result.deliveries().first()).isFalse();
    assertThat(result.deliveries().last()).isFalse();
    assertThat(result.deliveries().hasNext()).isTrue();
    assertThat(result.deliveries().hasPrevious()).isTrue();
  }

  private Payroll confirmedPayroll() {
    return Payroll.restore(1L, 10L, YearMonth.of(2026, 8), LocalDate.of(2026, 9, 5),
        CONFIRMED, new BigDecimal("3000000"), BigDecimal.ZERO, new BigDecimal("3000000"),
        1, null, null, LocalDateTime.now(), LocalDateTime.now(), 2, List.of());
  }

  private StatementData readyStatement() {
    return new StatementData(20L, 1L, READY, "Finance/Staff/statement.pdf",
        "application/pdf", 100L, "checksum", LocalDateTime.now(), null);
  }

  private DeliveryData delivery() {
    return new DeliveryData(30L, null, 20L, 1L, 10L, "staff@example.com", PENDING,
        null, null, "token", null, 99L, LocalDateTime.now(), null, null, null, null);
  }
}
