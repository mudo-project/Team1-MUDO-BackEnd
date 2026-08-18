package com.academy.mudogroupware.payroll.presentation.api;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.payroll.application.service.PayrollService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PayrollControllerTest {

  private final PayrollStatementEmailService emailService = mock(PayrollStatementEmailService.class);
  private final PayrollController controller = new PayrollController(
      mock(PayrollService.class), mock(PayrollStatementService.class), emailService);
  private final AuthUser authUser = new AuthUser(99L, "admin", 1L, "ADMIN");

  @Test
  void 새_발송_등록은_201을_반환한다() {
    when(emailService.send(1L, 99L)).thenReturn(
        new PayrollStatementEmailService.DeliveryResult(
            30L, 1L, PENDING, LocalDateTime.of(2026, 8, 6, 10, 0), false));

    var response = controller.sendStatementEmail(authUser, 1L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(response.getBody().data().reused()).isFalse();
  }

  @Test
  void 기존_발송_이력_재사용은_200을_반환한다() {
    when(emailService.send(1L, 99L)).thenReturn(
        new PayrollStatementEmailService.DeliveryResult(
            30L, 1L, PENDING, LocalDateTime.of(2026, 8, 6, 10, 0), true));

    var response = controller.sendStatementEmail(authUser, 1L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getBody().code()).isEqualTo("PAYROLL_200_17");
    assertThat(response.getBody().data().reused()).isTrue();
  }
}
