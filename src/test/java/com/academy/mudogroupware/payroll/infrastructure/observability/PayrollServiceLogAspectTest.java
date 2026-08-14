package com.academy.mudogroupware.payroll.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailPolicy;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PayrollServiceLogAspectTest {

  @Test
  void 발송_배치크기_조회는_시작과_완료를_DEBUG로_기록한다() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Signature signature = mock(Signature.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(PayrollStatementEmailPolicy.class);
    when(signature.getName()).thenReturn("dispatchBatchSize");
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn(20);

    List<ILoggingEvent> logs = captureLogs(
        () -> new PayrollServiceLogAspect().logServiceEvent(joinPoint));

    assertThat(logs).hasSize(2)
        .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    assertThat(logs.get(0).getFormattedMessage()).contains("dispatch_batch_size_시작");
    assertThat(logs.get(1).getFormattedMessage()).contains("dispatch_batch_size_완료");
  }

  private List<ILoggingEvent> captureLogs(ThrowingRunnable action) throws Throwable {
    Logger logger = (Logger) LoggerFactory.getLogger(PayrollServiceLogAspect.class);
    Level previousLevel = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.DEBUG);
    logger.addAppender(appender);
    try {
      action.run();
      return List.copyOf(appender.list);
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(previousLevel);
      appender.stop();
    }
  }

  private interface ThrowingRunnable {
    void run() throws Throwable;
  }
}
