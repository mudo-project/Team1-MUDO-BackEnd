package com.academy.mudogroupware.payroll.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;

class PayrollServiceLogAspectTest {

  @Test
  void 설정_정책은_서비스_로깅_포인트컷에서_제외한다() throws NoSuchMethodException {
    Method method = PayrollServiceLogAspect.class.getDeclaredMethod(
        "logServiceEvent", ProceedingJoinPoint.class);

    assertThat(method.getAnnotation(Around.class).value())
        .contains("!within(com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailPolicy)");
  }
}
